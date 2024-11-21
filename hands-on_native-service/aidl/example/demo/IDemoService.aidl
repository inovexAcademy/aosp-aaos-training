/*
 * Copyright (C) 2024 inovex
 * SPDX-License-Identifier: Apache-2.0
 */

package example.demo;


/** {@hide} */
interface IDemoService {

    /**
     * Returns HelloWorld and logs the call to logcat and stdout.
     * @return              "HelloWorld"
     */
    @utf8InCpp String getHelloWorld();

}
