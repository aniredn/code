#include "userprog/syscall.h"
#include <stdio.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"
#include "threads/vaddr.h"
#include "threads/malloc.h"
#include "threads/synch.h"
#include "userprog/pagedir.h"
#include "devices/shutdown.h"
#include "filesys/filesys.h"
#include "filesys/file.h"
#include <list.h>
#include "devices/input.h"
#include "userprog/process.h"

static void syscall_handler (struct intr_frame *);

static void sys_halt (struct intr_frame *f, void *cur_sp);
static void sys_exit (struct intr_frame *f, void *cur_sp);
static void sys_exec (struct intr_frame *f, void *cur_sp);
static void sys_wait (struct intr_frame *f, void *cur_sp);
static void sys_create (struct intr_frame *f, void *cur_sp);
static void sys_remove (struct intr_frame *f, void *cur_sp);
static void sys_open (struct intr_frame *f, void *cur_sp);
static void sys_filesize (struct intr_frame *f, void *cur_sp);
static void sys_read (struct intr_frame *f, void *cur_sp);
static void sys_write (struct intr_frame *f, void *cur_sp);
static void sys_seek (struct intr_frame *f, void *cur_sp);
static void sys_tell (struct intr_frame *f, void *cur_sp);
static void sys_close (struct intr_frame *f, void *cur_sp);

static bool invalid_pointer (const void *p);
static struct file *find_file(int fd);

static struct lock fd_lock;
static struct lock read_lock;
static struct lock write_lock;

#define VALIDATE_POINTER(cur_sp,var,f)  \
({ if (invalid_pointer (cur_sp))        \
 {                                      \
   syscall_thread_exit (f, -1);         \
   return;                              \
 }                                      \
 var = *(typeof (var)*)cur_sp;          \
})

void
syscall_init (void) 
{
  lock_init (&fd_lock);
  lock_init (&write_lock);
  lock_init (&read_lock);
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

static bool invalid_pointer(const void *p)
{
  if (!is_user_vaddr(p)||!pagedir_get_page(thread_current()->pagedir,p)||p==NULL) 
    {
      return true;
    }
  return false;
}

static struct file *find_file (int fd)
{
  struct thread *t = thread_current();
  struct list_elem *e;
  for (e=list_begin(&t->file_list);e!=list_end(&t->file_list);e=list_next(e))
    {
      struct file_elem *f_elem = list_entry(e, struct file_elem, elem);
      if (f_elem->fd == fd)
      {
        return f_elem->file;
      }
    }
  return NULL;
}

void thread_exit_all (int status) 
{
  printf ("%s: exit(%d)\n", thread_name (), status);
  struct thread *t = thread_current ();
  struct list_elem *e;
  
  while (!list_empty (&t->child_list)) 
    {
      e = list_pop_back(&t->child_list);
      struct child_elem *c_elem = list_entry(e, struct child_elem, elem);
      free_thread_from_exit_list(c_elem->pid);
      free(c_elem);
    }

    while (!list_empty (&t->file_list))
    {
      e = list_pop_back (&t->file_list);
      struct file_elem *f_elem = list_entry (e, struct file_elem, elem);
      file_close (f_elem->file);
      free (f_elem);
    }

  while (!list_empty (&t->child_list_waiting))
    {
      e = list_pop_back (&t->child_list_waiting);
      struct child_wait_elem *w_elem = list_entry (e, struct child_wait_elem, elem);
      free (w_elem);
    }

  add_thread_to_exited_list (t->tid, status);
  
  if (t->exec_file) 
    {
      file_allow_write (t->exec_file);
      file_close (t->exec_file);
    }

  while (!list_empty (&t->acquired_locks))
    {
      struct list_elem *e = list_front (&t->acquired_locks);
      struct lock *l = list_entry (e, struct lock, elem);
      lock_release (l);
    }

  struct thread *parent = get_thread (thread_current()->parent_id);
  if (parent)  
  {
    sema_up (&parent->parent_waiting_on_child);
  }
  thread_exit ();
}

void syscall_thread_exit (struct intr_frame *f, int status)
{
  thread_exit_all (status);
  f->eax = status;
}

static void syscall_handler (struct intr_frame *f) 
{
  int syscall_num;
  VALIDATE_POINTER (f->esp, syscall_num, f);
  void *cur_sp = f->esp + sizeof (void *);

  switch (syscall_num)
    {
      case SYS_HALT:
       sys_halt (f, cur_sp);
        break;
      case SYS_EXIT:
        sys_exit (f, cur_sp);
        break;
      case SYS_EXEC:
        sys_exec (f, cur_sp);
        break;
      case SYS_WAIT:
         sys_wait (f, cur_sp);
         break;
      case SYS_CREATE:
        sys_create (f, cur_sp);
        break;
      case SYS_REMOVE:
        sys_remove (f, cur_sp);
        break;
      case SYS_OPEN:
        sys_open (f, cur_sp);
        break;
      case SYS_FILESIZE:
        sys_filesize (f, cur_sp);
        break;
     case SYS_READ:
       sys_read (f, cur_sp);
        break;
      case SYS_WRITE:
        sys_write (f, cur_sp);
        break;
      case SYS_SEEK:
        sys_seek (f, cur_sp);
        break;
      case SYS_TELL:
        sys_tell (f, cur_sp);
        break;
      case SYS_CLOSE:
        sys_close (f, cur_sp);
        break;
      default :
        printf ("Invalid system call! #%d\n", syscall_num);
        syscall_thread_exit (f, -1);
        break;
    }
}

static void sys_halt (struct intr_frame *f UNUSED, void *cur_sp UNUSED)
{
  shutdown_power_off ();
}

static void sys_exit (struct intr_frame *f, void *cur_sp)
{
  int status;
  VALIDATE_POINTER (cur_sp, status, f); 
  syscall_thread_exit (f, status);
}

static void sys_exec (struct intr_frame *f, void *cur_sp)
{
  const char *cmd_line;
  VALIDATE_POINTER (cur_sp, cmd_line, f);

  if (!is_user_vaddr (cmd_line)||!pagedir_get_page (thread_current ()->pagedir, cmd_line)||cmd_line == NULL)
    {
      syscall_thread_exit (f, -1);
      return;
    }
  
  tid_t pid = process_execute (cmd_line);

  f->eax = pid;
}

static void
sys_wait (struct intr_frame *f, void *cur_sp)
{
  tid_t pid;
  VALIDATE_POINTER (cur_sp, pid, f);

  struct thread *t = thread_current ();
  
  struct list_elem *e;
  for (e = list_begin (&t->child_list); e != list_end (&t->child_list);e = list_next (e))
    {
      struct child_elem *c_elem = list_entry (e, struct child_elem, elem);
      if (c_elem->pid == pid)
        {
          struct list_elem *waited_e;
          for(waited_e = list_begin (&t->child_list_waiting); waited_e != list_end (&t->child_list_waiting);waited_e = list_next (waited_e))
          {
             struct child_wait_elem *waited_c_elem = list_entry (waited_e, struct child_wait_elem, elem);
             if (waited_c_elem->pid == pid)
               {
                  f->eax = -1;
                  return;
                }
            }
         
          struct child_wait_elem *new_wait_c_elem;

          new_wait_c_elem = (typeof (new_wait_c_elem)) malloc (sizeof (struct child_wait_elem));
            if (new_wait_c_elem == NULL)
            {
                syscall_thread_exit (f, -1);
            }
          new_wait_c_elem->pid = pid;
          list_push_back (&t->child_list_waiting, &new_wait_c_elem->elem); 
          f->eax = process_wait (pid); 
          return;
        }
    }
  f->eax = -1;
}

static void sys_create (struct intr_frame *f, void *cur_sp)
{
  const char *file;
  unsigned size;
  VALIDATE_POINTER (cur_sp, file, f);
  cur_sp =cur_sp + sizeof (void *);
  VALIDATE_POINTER (cur_sp, size, f);

  if (!is_user_vaddr (file)||!pagedir_get_page (thread_current ()->pagedir, file)||file == NULL)
    {
      syscall_thread_exit (f, -1);
      return;
    }

  f->eax = filesys_create (file,size);
}

static void sys_remove (struct intr_frame *f, void *cur_sp)
{
  const char *file;
  VALIDATE_POINTER (cur_sp, file, f);

  if (!is_user_vaddr (file)||!pagedir_get_page (thread_current ()->pagedir, file)||file == NULL)
    {
      syscall_thread_exit (f, -1);
      return;
    }

  f->eax = filesys_remove (file);
}


static void sys_open (struct intr_frame *f, void *cur_sp)
{
  static int next_fd = 2;
  int fd;
  const char *file_name;
  VALIDATE_POINTER (cur_sp, file_name, f);
  
  if (!is_user_vaddr (file_name)||!pagedir_get_page (thread_current ()->pagedir, file_name)||file_name == NULL)
    {
      syscall_thread_exit (f, -1);
      return;
    }

  struct file *file = filesys_open (file_name);
  if (!file)
    fd = -1;
  else
    {
      lock_acquire (&fd_lock);
      fd = next_fd++;
      lock_release (&fd_lock);
    }
  struct thread *t = thread_current ();
  struct file_elem *f_elem;
 
  f_elem = (typeof (f_elem)) malloc (sizeof (struct file_elem));
  if (f_elem == NULL)
   {
     syscall_thread_exit (f, -1);
   }


  f_elem->fd =fd;
  f_elem->file = file;

  list_push_back (&t->file_list, &f_elem->elem);

  f->eax = fd;
}

static void sys_filesize (struct intr_frame *f, void *cur_sp)
{
  int fd;
  VALIDATE_POINTER (cur_sp, fd, f);

  struct file *file = find_file (fd);
  if (file != NULL)
    {
      f->eax = file_length (file);
    }
  else
    syscall_thread_exit (f, -1);
}

static void sys_read (struct intr_frame *f, void *cur_sp)
{
  int fd;
  void * buffer;
  unsigned length;
  VALIDATE_POINTER (cur_sp, fd, f);
  cur_sp += sizeof (void *);
  VALIDATE_POINTER (cur_sp, buffer, f);
  cur_sp += sizeof (void *);
  VALIDATE_POINTER (cur_sp, length, f);

 
  if (fd == STDOUT_FILENO || fd < -1 ||invalid_pointer (buffer) ||invalid_pointer (buffer + length))
    {
      syscall_thread_exit (f, -1);
      return;
    }

  void *buffer_tmp_ptr = buffer + PGSIZE;
  while (buffer_tmp_ptr < buffer + length)
    {
      if (!is_user_vaddr (buffer_tmp_ptr)||!pagedir_get_page (thread_current ()->pagedir, buffer_tmp_ptr)||buffer_tmp_ptr == NULL)
    {
      syscall_thread_exit (f, -1);
      return;
    }      

      buffer_tmp_ptr += PGSIZE;
    }

  ASSERT (fd >= 0);
  
  if (fd == STDIN_FILENO) 
    {
      f->eax = input_getc ();
      return;
    }
  
  struct file *file = find_file (fd);
  if (file != NULL)
    {
      lock_acquire (&read_lock);
      f->eax = file_read (file, buffer, length);
      lock_release (&read_lock);
    }
  else
    syscall_thread_exit (f, -1);
}

static void sys_write (struct intr_frame *f, void *cur_sp)
{
  int fd;
   void * buffer;
  unsigned length;
  VALIDATE_POINTER (cur_sp, fd, f);
  cur_sp += sizeof (void *);
  VALIDATE_POINTER (cur_sp, buffer, f);
  cur_sp += sizeof (void *);
  VALIDATE_POINTER (cur_sp, length, f);

  if (fd == STDIN_FILENO || fd < -1 || invalid_pointer (buffer) || invalid_pointer (buffer + length))
    {
      syscall_thread_exit (f, -1);
      return;
    }

  void *buffer_tmp_ptr = buffer + PGSIZE;
  while (buffer_tmp_ptr < buffer + length)
    {
      if (!is_user_vaddr (buffer_tmp_ptr)||!pagedir_get_page (thread_current ()->pagedir, buffer_tmp_ptr)||buffer_tmp_ptr == NULL)
    {
      syscall_thread_exit (f, -1);
      return;
    }     
      buffer_tmp_ptr += PGSIZE;
    }

  if (fd == STDOUT_FILENO)
    {
      putbuf (buffer, length);
      f->eax = length;
      return;
    }

  struct file *file = find_file (fd);
  if (file != NULL)
    {
      lock_acquire (&read_lock);
      lock_acquire (&write_lock);
      f->eax = file_write (file, buffer, length);
      lock_release (&write_lock);
      lock_release (&read_lock);
    }
  else
    syscall_thread_exit (f, -1);
}

static void sys_seek (struct intr_frame *f, void *cur_sp) 
{
  int fd;
  unsigned position;
  VALIDATE_POINTER (cur_sp, fd, f);
  cur_sp = cur_sp + sizeof (void *);
  VALIDATE_POINTER (cur_sp, position, f);

  struct file *file = find_file (fd);
  if (file != NULL)
    {
      lock_acquire (&read_lock);
      lock_acquire (&write_lock);
      file_seek (file, position);
      lock_release (&write_lock);
      lock_release (&read_lock);
    }
  else
    syscall_thread_exit (f, -1);
}

static void sys_tell (struct intr_frame *f, void *cur_sp) 
{
  int fd;
  VALIDATE_POINTER (cur_sp, fd, f);

  struct file *file = find_file(fd);
  if (file != NULL)
    {
      lock_acquire (&read_lock);
      lock_acquire (&write_lock);
      f->eax = file_tell (file);
      lock_release (&write_lock);
      lock_release (&read_lock);
    }
  else
    syscall_thread_exit (f, -1);
}

static void sys_close (struct intr_frame *f, void *cur_sp)
{
  int fd;
  VALIDATE_POINTER (cur_sp, fd, f);

  struct thread *t = thread_current();
  struct list_elem *e;
  for (e=list_begin (&t->file_list);e!=list_end(&t->file_list);e =list_next(e))
    {
      struct file_elem *f_elem = list_entry(e, struct file_elem, elem);
      if (f_elem->fd == fd)
        {
          file_close (f_elem->file);
          list_remove (e);
          free (f_elem);
          return;
        }
    }  
 syscall_thread_exit (f, -1); 
}
