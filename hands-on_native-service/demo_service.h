/*
 * Copyright (C) 2024 inovex
 * SPDX-License-Identifier: Apache-2.0
 */
#pragma once

#include <string>

#include <binder/BinderService.h>

#include <example/demo/BnDemoService.h>

namespace example {
namespace demo {

static constexpr char kDemoServiceName[] = "demoservice";

class DemoService : public android::BinderService<DemoService>, public BnDemoService {
  public:
    static void RegisterService(const char* name);

    android::binder::Status getHelloWorld(std::string* _aidl_return);

    DemoService();
};

}  // namespace demo
}  // namespace example
