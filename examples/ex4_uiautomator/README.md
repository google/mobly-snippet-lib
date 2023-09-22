# UIAutomator Snippet Example

The UiAutomator API, which allows developers to automate UI interactions on an
Android device, is now available in Python on
[google/snippet-uiautomator](https://github.com/google/snippet-uiautomator).
This makes it possible for developers to use UiAutomator in Mobly tests without
having to write Java.

The `snippet-uiautomator` package is a wrapper around the AndroidX UiAutomator
APIs. It provides a Pythonic interface for interacting with Android UI elements,
such as finding and clicking on buttons, entering text into fields, and
scrolling through lists.

To use the `snippet-uiautomator` package, developers simply need to install it
from PyPI and import it into their Python code. Once imported, they can use the
package's API to automate any UI interaction.

Here is an example of how to use the `snippet-uiautomator` package to automate a
simple UI interaction:

```Python
from mobly.controllers import android_device
from snippet_uiautomator import uiautomator

# Connect to an Android device.
ad = android_device.AndroidDevice(serial)

# Load UiAutomator service.
uiautomator.load_uiautomator_service(ad)

# Find the "Login" button.
button = ad.ui(res='com.example.app:id/login_button')

# Click on the button.
button.click()
```
