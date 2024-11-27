# Hands-On: Implement a native example service

In the Android.bp file are two binaries defined the `demo` binary
implementing the demoservice. And the `demo-client` util which connects
over binder to the demoservice.


## Limitations:

 - The example service ignores all permission handling.
 - No auto start over the init deamon.
 - The service needs manual deployment, it is not added to a device/product.
 - No integration of the service's git repo into the build tree with `repo`.


## Hints

 - To format the source code run `clang-format -i *.cpp *.h`.


# Step 1: Service setup and initial build

1. Clone or copy the `hands-on_native-service` folder to `system/demo` in the AOSP source tree.

2. Make sure the service `demo` and `demo-client` util builds without errors.


Verify:

If the service is build correctly you can find it's binary under:
`out/target/product/trout_x86_64/system/bin/demo`
and
`out/target/product/trout_x86_64/system/bin/demo-client`


Hints:

 - The service is not added to a device/product.
 - Remember `hmm` to show option on how to build a single module.


# Step 2: Run the service on a (virtual) device

As the service is not integrated into an product it is not automatically
installed.

To install it by hand the system partition needs to be writable.

```
adb root
# Only needed once (`adb reboot` required)
adb disable-verity
adb remount system
adb sync system
```

And then run the service manually `demo`.
In a new window and `adb shell` session test it with the `demo-client hello`.

Call the service with the `service call ...` util.
See the documentation of the `service` util on how the call feature works.

If you are curious on how the generated code for the AIDL file looks, you can
find it under:
`out/soong/.intermediates/system/demo/demo_aidl_interface-cpp-source/gen/include/example/demo`


Verify:

 - The running service is shown in `service list`.
    (If the service list command hangs stop it with CTRL-C)
 - Running `demo-client hello` works.
 - `service call ...` works.


# Step 3: Log to the system log buffers (logcat)

Currently the service only logs to the standard output.
But if the service is running under the `init` daemon these logs would not show
up in the logcat.

Add logging to the system logcat buffer to the service calls.


There is a C library liblog.
And the C++ logger with `android-base/logging.h` in the libbase.

See the readme of the liblog library [/system/logging/liblog/README.md](https://cs.android.com/android/platform/superproject/main/+/main:system/logging/liblog/README.md).
See the readme of the libbase library [/system/libbase/README.md](https://cs.android.com/android/platform/superproject/main/+/main:system/libbase/README.md).


## Verify

The added log messages appearer in `adb logcat`.


## Hints

 - Check how other services do the logging.
 - Logcat supports log filters see `logcat` on how to reduce the log output.


# Step 4: Implement a new AIDL method

Add the function `int calculate(int a, int b)`
to the AIDL interface and implement it.

This function should add the two numbers (a+b) and return the result.

Extend the client part of the service to call the new function.


## Verify

With the `service` command you can call any service provided over the service manager.
Call the new service implementation. (The `service` command has a help / usage message.)


## Hints

 - Check other services or the AIDL documentation how to add the function.


# Step 5: (Optional) Add the service to product and init system


For the init system see:
 - Other services (e.g system/gsid).
 - https://android.googlesource.com/platform/system/core/+/master/init/README.md

To add the service binary `demo` and the `demo-client` to the Android product see:
 - `device/google/trout/` the `aosp_trout_x86_64.mk` and `aosp_trout_common.mk`.
 - And add the module to the `PRODUCT_PACKAGES`.


Verify:
 - Service is automatically build with `m`.
    (You can delete the service binary manually)
 - Service is started by the init daemon. Check with `demo call-service`

