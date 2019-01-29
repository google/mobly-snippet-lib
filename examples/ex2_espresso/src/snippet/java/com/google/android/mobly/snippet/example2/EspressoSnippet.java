/*
 * Copyright (C) 2016 Google Inc.
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

package com.google.android.mobly.snippet.example2;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.action.ViewActions;
import androidx.test.rule.ActivityTestRule;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;
import org.junit.Rule;

public class EspressoSnippet implements Snippet {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Rpc(description="Opens the main activity of the app")
    public void startMainActivity() {
        mActivityRule.launchActivity(null /* startIntent */);
    }

    @Rpc(description="Pushes the main app button, and checks the label if this is the first time.")
    public void pushMainButton(boolean checkFirstRun) {
        if (checkFirstRun) {
            onView(withId(R.id.main_text_view)).check(matches(withText("Hello World!")));
        }
        onView(withId(R.id.main_button)).perform(ViewActions.click());
        if (checkFirstRun) {
            onView(withId(R.id.main_text_view)).check(matches(withText("Button pressed 1 times.")));
        }
    }

    @Override
    public void shutdown() {
        mActivityRule.getActivity().finish();
    }
}
