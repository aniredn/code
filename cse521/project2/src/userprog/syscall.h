#ifndef USERPROG_SYSCALL_H
#define USERPROG_SYSCALL_H

#include <list.h>
#include "threads/interrupt.h"
#include "threads/thread.h"

struct file_elem // Used for file descriptor list insertions
  {
    int fd;                  // file descriptor
    struct file *file;
    struct list_elem elem;
  };

void syscall_init (void);

void thread_exit_all (int status);

void syscall_thread_exit (struct intr_frame *f, int status);

#endif /* userprog/syscall.h */