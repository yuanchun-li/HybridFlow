# Cross-Origin Info Flow of Android App (draft)

## About

Mobile Applications are mostly made up with code from different origins. Unlike tranditional browsers which properly manage  origins by Same Origin Policy (SOP), mobile apps usually can not serve as Trusted Computing Base (TCB) and allows free access of data between origins, which put users and benign origins in potential risks. In this work, we exposure such vulnerabilities by analyzing the information flow between different origins.

## Origin Definition and Classification

### Webpage Origin & Package Origin

For web pages, an origin is defined as a combination of URI scheme, hostname, and port number. In our work, we define the origin of a package as the organization which the package authors belong to.

### Host Origin & Guest Origin

Host origins include the origin of main package and the origins of the authors' own web pages.
Guest origins include the origins of third-party packages and the origins of third party web pages.

## Risk Examples

Packages distributed by different origins share the same sandbox isolation in one app, package A from one origin have access to resources of another package B, including permissions granted to package B and the local data stored by package B. Thus we have the following risky information flow situations:

1. Host Origin --> Guest Package Origin
	Third party library get location of device, and the location permission is supposed only granted to host package. Eg. Ad Library. Many researches involved.
2. Host Origin <-- Guest Package Origin
	Guest package store some private data locally, host package have free access to it. Eg. The Facebook SDK stores cookie in app's local storage upon Signal Sign On, and host package read the cookie and use it to have full access to user's Facebook account.

HTML pages from different origins share the same WebView container. The webpages have access to the javascript interfaces of WebView container, and the WebView client also have access to all webpages rendered inside.

3. Host Origin --> Guest Webpage Origin
	Like in 1, host app add several Javascript Interfaces, the interfaces are intended to be used by host webpage only. However, guest webpages often also have access to them. Eg. Many related work.
4. Host Origin <-- Guest Webpage Origin
	The content of webpage is important and should not been seen or modified by the app, but host package can easily do this. Eg. In Waicai app, a guest webpage contains a DOM element for user to input password, and the app get the input of the DOM element by executing a js in WebView.