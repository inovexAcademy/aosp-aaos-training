#include "aosp_random.h"

#include <linux/module.h>

int get_random_number(void)
{
	return 4; // chosen by fair dice roll. Guaranteed to be random, see https://xkcd.com/221/
}

EXPORT_SYMBOL_GPL(get_random_number);

MODULE_LICENSE("GPL");
