/*
 * Copyright (C) 2024 inovex
 * SPDX-License-Identifier: Apache-2.0
 */

#include <demo_service.h>
#include <example/demo/IDemoService.h>

#include <unistd.h>
#include <string>

#include <android-base/logging.h>
#include <binder/IServiceManager.h>

namespace example {
namespace demo {

DemoService::DemoService() {}

/**
 * Registers the service to the Android ServiceManager.
 *
 * Verify with the `service list` cli command.
 */
void DemoService::RegisterService(const char* name) {
    android::sp<DemoService> service = new DemoService();

    android::sp<android::IServiceManager> sm = android::defaultServiceManager();
    if (sm == nullptr) {
        std::cout << "Unable to get default service manager!" << "\n";
        return;
    }

    android::status_t status = sm->addService(android::String16(name), service);
    if (status != android::OK) {
        std::cout << "Failed to add service to ServiceManager: " << name << "\n";
    }
}

/**
 * getHelloWorld is inoveked by a remote service over binder.
 */
android::binder::Status DemoService::getHelloWorld(std::string* _aidl_return) {
    std::cout << "getHelloWorld called";

    *_aidl_return = "Hello World";
    return android::binder::Status::ok();
}

}  // namespace demo
}  // namespace example
