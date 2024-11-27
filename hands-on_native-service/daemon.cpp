// Copyright (C) 2024 inovex
// SPDX-License-Identifier: Apache-2.0

#include <iostream>

#include <android-base/logging.h>
#include <binder/BinderService.h>
#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/ProcessState.h>

#include "demo_service.h"

using android::ProcessState;
using android::sp;
using namespace std::literals;

int main() {
    std::cout << "Register the `demoservice` to the service manager\n";

    example::demo::DemoService::RegisterService(example::demo::kDemoServiceName);

    sp<android::ProcessState> ps(ProcessState::self());
    ps->startThreadPool();
    ps->giveThreadPoolName();

    std::cout << "Service demoservice is registerd\n";

    android::IPCThreadState::self()->joinThreadPool();

    exit(0);
}
