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

    std::cout << "Connecting to demo-service\n";

    static android::sp<android::IBinder> res = sm->waitForService(name);
    if (res) {
        return android::interface_cast<example::demo::IDemoService>(res);
    }

    std::cout << "Unable to GetDemoService\n";

    return nullptr;
}

/**
 * Connect to the DemoService as client and call getHelloWorld.
 */
static int DemoServiceCall() {
    auto service = GetDemoService();
    if (!service) {
        std::cerr << "Could not start IDemoService.\n";
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
    if (argc > 1) {
        if (argv[1] == "call-service"s) {
            int rc = DemoServiceCall();
            exit(rc);
        }
        std::cerr << "Command not found, use 'call-service'.\n";
        exit(0);
    }

    std::cout << "Register to service manager\n";

    example::demo::DemoService::RegisterService(example::demo::kDemoServiceName);
    {
        sp<android::ProcessState> ps(ProcessState::self());
        ps->startThreadPool();
        ps->giveThreadPoolName();
    }

    std::cout << "Service demoservice is registerd\n";

    android::IPCThreadState::self()->joinThreadPool();

    exit(0);
}
