# Android Microsoft sign-in configuration

`client ID` identifies the Microsoft application that requests sign-in, not the
player's Minecraft account. Android 0.4.1 shipped without a configured application
ID and therefore asks for one under Advanced before it can request a device code.

The build owner must register their application for personal Microsoft accounts,
enable public-client/device-code flows and obtain Minecraft API access from
Mojang. These are account/provider settings, not values that the game can invent.
See [Microsoft account setup](microsoft-account.md#what-ludovic-registers-needs-his-accounts)
and the [Minecraft App ID review](https://aka.ms/mce-reviewappid).
Never substitute another launcher's application ID.

Once the owner's application is ready, set the repository **Actions variable**
`HORIZON_MSA_CLIENT_ID` to its public Application (client) ID. The Android workflow
passes it to the APK builder, which validates the UUID and embeds
`assets/horizon-auth.json`. Ordinary players then use the Sign in button without
entering an ID. An existing per-install ID remains an override, so a refresh token
continues to be used with the application that issued it.

Client IDs are public configuration; no client secret belongs in this APK.
Microsoft passwords, device codes and tokens must not be sent to the build owner
or an AI assistant. The player completes authorization on Microsoft's own site.

The current APK also accepts the owner's approved ID under Advanced without a
reinstall. Building a new APK without an application ID or without provider
approval cannot make online-mode Microsoft authentication work.
