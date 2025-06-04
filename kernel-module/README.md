Android Training Hands-on: Kernel Hacking
=========================================

Welcome to the inovex Android Training repository.
This repository contains the source code utilized in the hands-on exercises in the kernel pert of the training.

The kernel modules included in this repository serve specific purposes:
one "driver" module creates read-only sysfs entries, while a "library" module provides a function for generating pseudo-random numbers.
The driver module will leverage the library module to produce some of the content displayed when the sysfs entries are accessed.


By exploring this repository, you will gain insights into various aspects of Android kernel development, including:

* Creating custom Android kernel modules
* Utilizing kleaf/bazel and the Driver Development Kit (DDK)
* Managing dependencies between kernel modules
* Controlling the visibility of symbols between modules

Detailed task descriptions, comprehensive build instructions, and additional resources will be provided by the instructors during the training sessions.

Please note that this repository is intended for educational purposes only and is not a template for production-ready code.
