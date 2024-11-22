// Copyright (C) 2024 inovex
// SPDX-License-Identifier: Apache-2.0

#include <iostream>
#include <string>

#include <android-base/logging.h>
#include <binder/BinderService.h>
#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/ProcessState.h>

#include <example/demo/IDemoService.h>

#include "demo_service.h"

using android::ProcessState;
using android::sp;
using namespace std::literals;

sp<example::demo::IDemoService> GetDemoService() {
    auto sm = android::defaultServiceManager();
    auto name = android::String16(example::demo::kDemoServiceName);

    std::cout << "Connecting to demoservice\n";

    static android::sp<android::IBinder> res = sm->waitForService(name);
    if (res) {
        return android::interface_cast<example::demo::IDemoService>(res);
    }

    std::cout << "Unable to get demoservice\n";

    return nullptr;
}

/**
 * Connect to the DemoService as client and call getHelloWorld.
 */
static int callGetHelloWorld() {
    auto service = GetDemoService();
    if (!service) {
        std::cerr << "Could not connect to IDemoService.\n";
        return 1;
    }

    std::string output;
    auto status = service->getHelloWorld(&output);
    if (!status.isOk()) {
        std::cerr << "Could not call getHelloWorld: " << status.exceptionMessage().c_str() << "\n";
        return 1;
    }
    std::cout << "Service Response: " << output << "\n";
    return 0;
}

int main(int argc, char** argv) {
    std::cout << "demo-client is a util to access the demoservice over binder\n";

    if (argc > 1) {
        if (argv[1] == "hello"s) {
            int rc = callGetHelloWorld();
            exit(rc);
        }
    }

    std::cerr << "Command not found, use 'demo-client hello'.\n";
    exit(0);
}
