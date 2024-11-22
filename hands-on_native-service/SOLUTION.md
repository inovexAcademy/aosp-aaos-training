# Solution Hands-On: Implement a native example service


# Step 1

```
cd system/demo
mm demo
```

# Step 2

```
$ service call demoservice 1
Result: Parcel(
0x00000000: 00000000 0000000b 00650048 006c006c '........H.e.l.l.'
0x00000010: 0020006f 006f0057 006c0072 00000064 'o. .W.o.r.l.d...')
```


# Step 3 - Logging

```
#include <android-base/logging.h>

int main(...) {
    android::base::InitLogging(argv, android::base::LogdLogger(android::base::SYSTEM));
    ...
}

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
