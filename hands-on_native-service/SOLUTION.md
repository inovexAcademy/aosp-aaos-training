# Solution Hands-On: Implement a native example service


# Step 1

```bash
cd system/demo
mm demo
```

# Step 2

```bash
$ service call demoservice 1
Result: Parcel(
0x00000000: 00000000 0000000b 00650048 006c006c '........H.e.l.l.'
0x00000010: 0020006f 006f0057 006c0072 00000064 'o. .W.o.r.l.d...')
```


# Step 3 - Logging

There are two ways to log in C/C++ services.
The C style logging,
and the C++ logger with `android-base/logging.h` in the libbase.


C++
```c++
#include <android-base/logging.h>

int main(...) {
    android::base::InitLogging(argv, android::base::LogdLogger(android::base::SYSTEM));
    ...

    LOG(INFO) << "Info log";
}
```


C Style
```c
ALOGI("Register to service manager");
ALOGI("Service demoservice is registerd");
ALOGE("Unable to get default service manager!");
ALOGE("Failed to add service to ServiceManager: %s", name);
```

Logcat output
```
11-19 14:49:25.882  9610  9610 I demo    : Register to service manager
11-19 14:49:25.883  9610  9610 I demo    : Service demoservice is registerd
```


# Step 4 - AIDL Function

Add the calculate function to the `aidl/example/demo/IDemoService.aidl`.
```java
int calculate(int a, int b);
```

Extend the `demo_service.h`:
```c++
android::binder::Status calculate(int32_t a, int32_t b, int32_t* _aidl_return);
```

Extend the `demo_service.cpp`:
```c++
android::binder::Status DemoService::calculate(int32_t a, int32_t b, int32_t* _aidl_return) {
    std::cout << "calculate called";

    *_aidl_return = a + b;
    return android::binder::Status::ok();
}
```


# Step 5 - Add the service to product and init system

Add the services to `packages/services/Car/car_product/build/car_system_ext.mk`.
```bash
PRODUCT_PACKAGES += demo demo-client
```

Create a `demo.rc` init file within the root of the demo service source tree:
```bash
service demo /system_ext/bin/demo
    interface aidl demoservice
    class main
    user root
```

Extend the Android.bp file for the demo cc_binary:
```json
cc_binary {
    name: "demo",
...
    init_rc: [
        "demo.rc",
    ],
...
```

