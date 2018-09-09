# FileEx
Final Capstone Project in Udacity Android Nanodegree

## Screenshots

#### Custom navigation drawer
<img src="/screenshots/browser_navdrawer_1_place.png" width="250" />  <img src="/screenshots/browser_navdrawer_3_places.png" width="250" />

#### File browser
<img src="/screenshots/browser_folders.png" width="250" /> <img src="/screenshots/browser_music_files.png" width="250" /> <img src="/screenshots/browser_images.png" width="250" />

#### Favorite manager
<img src="/screenshots/fav_manager.png" width="250" /> <img src="/screenshots/fav_manager_swipe_to_remove.png" width="250" />

#### Favorite manager (edit/new)
<img src="/screenshots/fav_add_edit.png" width="250" /> <img src="/screenshots/fav_add_edit_user_input_check.png" width="250" /> <img src="/screenshots/fav_folder_chooser.png" width="250" />	 <img src="/screenshots/fav_icon_chooser.png" width="250" />

#### Permission handling on API level >23
<img src="/screenshots/permission_handling.png" width="250" /S>



## Features

#### User Interface
* Using AppBarLayout and Toolbar
* Custom NavigationDrawer (swiping in from left screen side)
	* exchanged default implementation (with NavigationView) for a custom layout using ConstraintLayout and RecyclerViews for (1) custom favorites from database and (2) phones storage places (extrernal storage, additional SD card, root folder), depending on what is available/readable
	* user favorites get populated with defaults (Camera/DCIM, downloads) on database creation time
	* showing free/used space of each phone storage with graphical representation
* **Varying layouts in RecyclerView** for different view types
	* music files show actual meta data (cover art, title, artist, length, ...) using JAudiotagger
	* remaining files use thumbnails with Glide and file name
	* more types coming soon
* Managing local favorite folders (loading all favorites from local SQLite database into RecyclerView via Loader from ContentProvider):
	* Swipe to remove favorites
	* Click to edit with custom transition on favorite's image (which both Activities have in common)
	* FloatingActionButton to add new favorite
	* Checking user inputs: selecting a folder and a favorite's name is mandatory (the folder's name is automatically selected as the default name after selecting folder)
	* Folder selection in new Activity with another instance of the folder explorer fragment
* Simple widget offering shortcuts to important phone storage places and local favorites (user customizable in future versions)


#### File Browser
* folder contents are loaded in background thread
* click handling: files can be viewed in associated default app (offering app selection if multiple apps available)
* the file browser lives in a **Fragment** to be able to use multiple file browser instances in multiple fragments (tablet layout, multiple tabs) in the future
* each Fragment holds its own browsing history
    * phone back button navigates back in browsing history
	* implemented via callback interface that checks if the fragment could handle the back button press (as long as the current active Fragment's browsing history is not empty) or not (in which case the back button is handled by the system's default behaviour)


#### Google Play Services
* All screens contain an advertisement from AdMob (Firebase integrated) and test ads on Emulators
* Analytics tracks user actions such as selecting elements from NavigationDrawer (without storing personal identifiable information such as folder names!)


#### Data Persistence
* **SQLite database** for local favorite folders for name, folder and custom image
	* data gets exposed via **Content Provider**
	* Views in Fragments/Activities get populated via **Loader**
* Saving instance state on rotation


#### More
* permission handling with popup on API version Marshmellow and above (API level >23)
* Support for accessibility using content descriptions
* RTL layout switching on all layouts
* using signing configuration (with keystore definition stored in configuration file not in github) to use installRelease from Gradle tasks



## Libraries

#### Third Party Libraries
* Glide for image loading
* JAudiotagger for audio meta data (handled in Gradle using jitpack.io)
* Butterknife for View binding
* Timber logging
* Firebase Core (Analytics)
* Firebase Ads


#### Google support/design
* ConstraintLayout
* support:design
* support-v4
* appcompat-v7
* RecyclerView


