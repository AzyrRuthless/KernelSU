#include <linux/kernel.h>
#include <linux/mm.h>
#include <linux/syscalls.h>
#include <linux/vmalloc.h>
#include <linux/kallsyms.h>
#include <linux/version.h>
#include <asm/unistd.h>
#include <asm/syscall.h>
#include <linux/reboot.h>
#include <linux/fcntl.h>
#include <linux/kthread.h>
#include <linux/delay.h>
#include <linux/namei.h>
#include <linux/uaccess.h>

#include "sucompat.h"
#include "ksu.h"

#ifdef CONFIG_KSU_TAMPER_SYSCALL_TABLE

#ifndef __NR_execveat
#define __NR_execveat 281
#endif

#ifdef CONFIG_COMPAT
#ifndef __NR_execve_compat
#define __NR_execve_compat 11
#endif
#ifndef __NR_execveat_compat
#define __NR_execveat_compat 387
#endif
#ifndef __NR_reboot_compat
#define __NR_reboot_compat 88
#endif
#ifndef __NR_fstatat64_compat
#define __NR_fstatat64_compat 327
#endif
#ifndef __NR_faccessat_compat
#define __NR_faccessat_compat 334
#endif
#endif

extern int ksu_handle_sys_reboot(int magic1, int magic2, unsigned int cmd,
				 void __user **arg);

extern int ksu_handle_execveat_sucompat(int *fd, struct filename **filename_ptr,
					void *__never_use_argv, 
					void *__never_use_envp,
					int *__never_use_flags);

static long handle_execve_wrapper(long (*original_syscall)(), int *dfd,
				  const char __user *filename_user,
				  const char __user *const __user *argv,
				  const char __user *const __user *envp,
				  int *flags)
{
	struct filename *fname;
	struct filename *mod_fname;
	long ret;
	mm_segment_t old_fs;

	fname = getname(filename_user);
	if (IS_ERR(fname))
		return PTR_ERR(fname);

	mod_fname = fname;

	ksu_handle_execveat_sucompat(dfd, &mod_fname, (void *)argv, (void *)envp, flags);

	if (mod_fname != fname) {
		old_fs = get_fs();
		set_fs(KERNEL_DS);

		if (flags)
			ret = original_syscall(*dfd, mod_fname->name, argv, envp, *flags);
		else
			ret = original_syscall(mod_fname->name, argv, envp);

		set_fs(old_fs);
	} else {
		if (flags)
			ret = original_syscall(*dfd, filename_user, argv, envp, *flags);
		else
			ret = original_syscall(filename_user, argv, envp);
	}

	putname(fname);
	return ret;
}

#if LINUX_VERSION_CODE >= KERNEL_VERSION(4, 19, 0)
#else 

static long (*old_sys_reboot)(int, int, unsigned int, void __user *);
static long (*old_sys_execve)(const char __user *,
			      const char __user *const __user *,
			      const char __user *const __user *);
static long (*old_sys_execveat)(int, const char __user *,
			      const char __user *const __user *,
			      const char __user *const __user *, int);

static long (*old_sys_faccessat)(int, const char __user *, int);
static long (*old_sys_newfstatat)(int, const char __user *,
				  struct stat __user *, int);

static long hook_sys_execve(const char __user *filename,
			    const char __user *const __user *argv,
			    const char __user *const __user *envp)
{
    int dfd = AT_FDCWD;
    return handle_execve_wrapper((void*)old_sys_execve, &dfd, filename, argv, envp, NULL);
}

static long hook_sys_execveat(int dfd, const char __user *filename,
			    const char __user *const __user *argv,
			    const char __user *const __user *envp, int flags)
{
    return handle_execve_wrapper((void*)old_sys_execveat, &dfd, filename, argv, envp, &flags);
}

static long hook_sys_reboot(int magic1, int magic2, unsigned int cmd,
			    void __user *arg)
{
	void __user *arg_ptr = arg;
	ksu_handle_sys_reboot(magic1, magic2, cmd, &arg_ptr);
	return old_sys_reboot(magic1, magic2, cmd, arg_ptr);
}

static long hook_sys_faccessat(int dfd, const char __user *filename, int mode)
{
	ksu_handle_faccessat(&dfd, &filename, &mode, NULL);
	return old_sys_faccessat(dfd, filename, mode);
}

static long hook_sys_newfstatat(int dfd, const char __user *filename,
				struct stat __user *statbuf, int flag)
{
	ksu_handle_stat(&dfd, &filename, &flag);
	return old_sys_newfstatat(dfd, filename, statbuf, flag);
}

#ifdef CONFIG_COMPAT
static long (*old_compat_sys_reboot)(int, int, unsigned int, void __user *);
static long (*old_compat_sys_execve)(const char __user *, const void __user *,
				     const void __user *);
static long (*old_compat_sys_execveat)(int, const char __user *, const void __user *,
				     const void __user *, int);
static long (*old_compat_sys_faccessat)(int, const char __user *, int);
static long (*old_compat_sys_fstatat64)(int, const char __user *,
					struct stat64 __user *, int);

static long hook_compat_sys_execve(const char __user *filename,
				   const void __user *argv,
				   const void __user *envp)
{
    int dfd = AT_FDCWD;
    return handle_execve_wrapper((void*)old_compat_sys_execve, &dfd, filename, 
                                (const char __user *const __user *)argv, 
                                (const char __user *const __user *)envp, NULL);
}

static long hook_compat_sys_execveat(int dfd, const char __user *filename,
				   const void __user *argv,
				   const void __user *envp, int flags)
{
    return handle_execve_wrapper((void*)old_compat_sys_execveat, &dfd, filename,
                                (const char __user *const __user *)argv, 
                                (const char __user *const __user *)envp, &flags);
}

static long hook_compat_sys_reboot(int magic1, int magic2, unsigned int cmd,
				   void __user *arg)
{
	void __user *arg_ptr = arg;
	ksu_handle_sys_reboot(magic1, magic2, cmd, &arg_ptr);
	return old_compat_sys_reboot(magic1, magic2, cmd, arg);
}

static long hook_compat_sys_faccessat(int dfd, const char __user *filename,
				      int mode)
{
	ksu_handle_faccessat(&dfd, &filename, &mode, NULL);
	return old_compat_sys_faccessat(dfd, filename, mode);
}

static long hook_compat_sys_fstatat64(int dfd, const char __user *filename,
				      struct stat64 __user *statbuf, int flag)
{
	ksu_handle_stat(&dfd, &filename, &flag);
	return old_compat_sys_fstatat64(dfd, filename, statbuf, flag);
}
#endif /* CONFIG_COMPAT */

#endif /* Version Check */

static void **ksu_sys_call_table;
#ifdef CONFIG_COMPAT
static void **ksu_compat_sys_call_table;
#endif

static void replace_syscall_entry(void **table, int syscall_nr, void *new_func,
				  void *old_func_ptr_storage)
{
	struct page *page;
	unsigned long addr;
	void **writable_table_entry;
	void **table_entry;

	if (!table)
		return;

	table_entry = &table[syscall_nr];
	addr = (unsigned long)table_entry;

	if (old_func_ptr_storage) {
		void **storage = (void **)old_func_ptr_storage;
		if (!*storage) {
			*storage = *table_entry;
			pr_info("KernelSU: hooking syscall %d: 0x%p -> 0x%p\n",
				syscall_nr, *storage, new_func);
		}
	}

	if (is_vmalloc_addr(table_entry))
		page = vmalloc_to_page(table_entry);
	else
		page = virt_to_page(table_entry);

	if (!page) {
        pr_err("KernelSU: cannot get page for syscall table\n");
		return;
    }

	writable_table_entry = (void **)vmap(&page, 1, VM_MAP, PAGE_KERNEL);
	if (!writable_table_entry) {
        pr_err("KernelSU: vmap failed\n");
		return;
    }

	writable_table_entry = (void **)((unsigned long)writable_table_entry +
					 (addr & ~PAGE_MASK));

	preempt_disable();
	local_irq_disable();
	*writable_table_entry = new_func;
	local_irq_enable();
	preempt_enable();

	vunmap((void *)((unsigned long)writable_table_entry & PAGE_MASK));
}

void ksu_syscall_table_hook_init(void)
{
	ksu_sys_call_table = (void **)kallsyms_lookup_name("sys_call_table");
	if (!ksu_sys_call_table) {
		pr_err("KernelSU: sys_call_table not found\n");
		return;
	}

	replace_syscall_entry(ksu_sys_call_table, __NR_reboot, hook_sys_reboot,
			      &old_sys_reboot);
	replace_syscall_entry(ksu_sys_call_table, __NR_execve, hook_sys_execve,
			      &old_sys_execve);
	replace_syscall_entry(ksu_sys_call_table, __NR_execveat, hook_sys_execveat,
			      &old_sys_execveat);
	replace_syscall_entry(ksu_sys_call_table, __NR_faccessat,
			      hook_sys_faccessat, &old_sys_faccessat);
	replace_syscall_entry(ksu_sys_call_table, __NR_newfstatat,
			      hook_sys_newfstatat, &old_sys_newfstatat);

#ifdef CONFIG_COMPAT
	ksu_compat_sys_call_table =
		(void **)kallsyms_lookup_name("compat_sys_call_table");
	if (!ksu_compat_sys_call_table) {
		pr_warn("KernelSU: compat_sys_call_table not found\n");
	} else {
		replace_syscall_entry(ksu_compat_sys_call_table,
				      __NR_reboot_compat,
				      hook_compat_sys_reboot,
				      &old_compat_sys_reboot);
		replace_syscall_entry(ksu_compat_sys_call_table,
				      __NR_execve_compat,
				      hook_compat_sys_execve,
				      &old_compat_sys_execve);
		replace_syscall_entry(ksu_compat_sys_call_table,
				      __NR_execveat_compat,
				      hook_compat_sys_execveat,
				      &old_compat_sys_execveat);
		replace_syscall_entry(ksu_compat_sys_call_table,
				      __NR_faccessat_compat,
				      hook_compat_sys_faccessat,
				      &old_compat_sys_faccessat);
		replace_syscall_entry(ksu_compat_sys_call_table,
				      __NR_fstatat64_compat,
				      hook_compat_sys_fstatat64,
				      &old_compat_sys_fstatat64);
	}
#endif

    pr_info("KernelSU: syscall table tampering initialized\n");
}

#endif
