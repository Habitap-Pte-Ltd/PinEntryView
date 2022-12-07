### Creating a GPG Key
##### Important:
This section must be done only once for your Sonatype account.  You must keep the generated secret keys
(`secring.gpg` if following the steps below) in a safe place and ***must not upload it to some public
repository (e.g. Github)***.  You must use the same key for your subsequent publications to your account.

##### Steps:
1. Run the command below and choose/enter the following when prompted:
   - Kind: **(1) RSA and RSA (default)**
   - Key size: **4096**
   - Key validity: **(0) key does not expire**
   - User ID:
      - Real name: **Rex Mag-uyon Torres**
      - Email address: **rexmtorres@gmail.com**
      - Comment: *&lt;empty>*
   - Password: *&lt;Same as the Sonatype OSSRH account password>*<br>
     <br>

   ```shell
   > gpg --full-generate-key

   gpg: key 4B533CE55C73BFC8 marked as ultimately trusted
   gpg: revocation certificate stored as 'C:/Users/rexmt/AppData/Roaming/gnupg/openpgp-revocs.d\490C4A5284E5DBC68294DBE5C3F9D0D834417442.rev'
   public and secret key created and signed.

   pub   rsa4096 2021-09-15 [SC]
         490C4A5284E5DBC68294DBE5C3F9D0D834417442
   uid                      Habitap Pte. Ltd. <developers@habitap.app>
   sub   rsa4096 2021-09-15 [E]
   ```

2. Export and backup your secret key somewhere safe:

   ```shell
   > gpg --export-secret-keys 34417442 > C:\Users\rexmt\secring.gpg
   ```

3. Upload your public key to a public server so that sonatype can find it:

   ```shell
   > gpg --keyserver keyserver.ubuntu.com --send-keys 34417442
   ```

*Source: [7. Create a GPG key (Publishing a maven artifact 3/3: step-by-step instructions to MavenCentral publishing)](https://proandroiddev.com/publishing-a-maven-artifact-3-3-step-by-step-instructions-to-mavencentral-publishing-bd661081645d#e14e)*

<br>

### Setting up the Plugin for Publishing
Follow the steps indicated here: https://vanniktech.github.io/gradle-maven-publish-plugin/central/

<br>

### Publishing to Sonatype
1. Run `publish` Gradle task (`pinentry` -> `publishing` -> `publish`).

2. Login to OSSRH (https://s01.oss.sonatype.org/).

3. Locate your staging repository.

   <img src="https://central.sonatype.org/images/ossrh-build-promotion-menu.png">

4. Select your repository and click `Close` if it is not yet closed.

   <img src="https://central.sonatype.org/images/ossrh-staging-repo-close.png">

5. Once it's closed, the `Release` button will be enabled.  Click the `Release` button.

For detailed steps: https://central.sonatype.org/publish/release/
