#include <linux/module.h>

/* Some message statically returned when reading from the initial simple sysfs
 * entry.
 */
#define MESSAGE "Hello, Android\n"

/* Name of a directory that will be created below /sys/kernel.
 * This directory will contain our sysfs entries ("files").
 */
#define MODULE_SYSFS_DIR "aosp_training"

/* Name of the sysfs entry that just returns MESSAGE when read. */
#define MODULE_SYSFS_ENTRY_STRING read_me

/* Name of the sysfs entry that will return a pseudo-random number, eventually.
 * This entry does not exist when you start with the task. You will have to create it.
 */
#define MODULE_SYSFS_ENTRY_MAGIC magic_number

static struct kobject *training_kobj;
static ssize_t test_module_sysfs_show(struct kobject *, struct kobj_attribute *,
				      char *);

static struct kobj_attribute readme_attrib =
	__ATTR(MODULE_SYSFS_ENTRY_STRING, 0444, test_module_sysfs_show, NULL);

// TODO: Create a second static kobj_attribute variable to store the attributes
// of the MODULE_SYSFS_ENTRY_MAGIC sysfs entry
// You could use the same read function (test_module_sysfs_show) or create a
// second one. As you wish.


/**
 *	test_module_sysfs_show - return data to userspace when sysfs entry is read
 *
 * Returns the number of characters (bytes, usually) written to @buf.
 */
static ssize_t test_module_sysfs_show(struct kobject *kobj,
				      struct kobj_attribute *attr, char *buf)
{
	// Use emit. https://docs.kernel.org/filesystems/sysfs.html
	return sysfs_emit(buf, MESSAGE);
}

/**
 *	test_module_init - initialize the kernel module.
 *
 * This function is automatically called when loading the module.
 *
 * Returns 0 if the module setup passed without issues or an error code
 * otherwise.
 */
static int __init test_module_init(void)
{
	int error = 0;

	printk("module_init()");

	training_kobj = kobject_create_and_add(MODULE_SYSFS_DIR, kernel_kobj);
	if (!training_kobj) {
		return -ENOMEM;
	}

	error = sysfs_create_file(training_kobj, &readme_attrib.attr);
	if (error) {
		pr_err("failed to create sysfs entry");
	}

	// TODO: We need to create the second sysfs entry for MODULE_SYSFS_ENTRY_MAGIC.
	// Same pattern as above.
	// Not really relevant or important for the training, but in real world
	// code, this should be the moment to quickly spent a though on all the
	// nasty corner cases that occur if either of the entry creations fail...

	return error;
}

/**
 *	test_module_exit - tear down the kernel module.
 *
 * This function is automatically called when unloading the module.
 */
static void __exit test_module_exit(void)
{
	printk("module_exit()");
	sysfs_remove_file(training_kobj, &readme_attrib.attr);
	// TODO: Just to be complete, let's clean up behind us: Remove MODULE_SYSFS_ENTRY_MAGIC
	kobject_put(training_kobj);
}

module_init(test_module_init);
module_exit(test_module_exit);

MODULE_LICENSE("GPL");
