/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.mobly.snippet.rpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link AsyncRpc} annotation is used to annotate server-side implementations of RPCs that
 * trigger asynchronous events. This behaves generally the same as {@link Rpc}, but methods that are
 * annotated with {@link AsyncRpc} are expected to take the extra parameter which is the ID to use
 * when posting async events.
 *
 * <p>Sample Usage:
 *
 * <pre>{@code
 * {@literal @}AsyncRpc(description = "An example showing the usage of AsyncRpc")
 * public void doSomethingAsync(String callbackId, ...) {
 *     // start some async operation and post a Snippet Event object with the given callbackId.
 * }
 * }</pre>
 *
 * AsyncRpc methods can still return serializable values, which will be transported in the regular
 * return value field of the Rpc protocol.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AsyncRpc {
    /** Returns brief description of the function. Should be limited to one or two sentences. */
    String description();
}
