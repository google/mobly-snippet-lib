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

import com.google.android.mobly.snippet.Snippet;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation will cause the RPC to execute on the main app thread.
 *
 * <p>This annotation can be applied to:
 *
 * <ul>
 *   <li>The constructor of a class implementing the {@link Snippet} interface.
 *   <li>A method annotated with the {@link Rpc} or {@link AsyncRpc} annotation.
 *   <li>The {@link Snippet#shutdown()} method.
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RunOnUiThread {}
