# 2016-10-09 Sun 16:03
Created the app!
The idea is to 'hear colour' a la Neil Harbisson, pulling colour from the camera, mapping it to a sound frequency, and playing it on the speaker.

Two modes of operation:

##1. Hear Hue
Visual input (hue) -> auditory output (tone)

  1.1 Visual input
  Initially: live camera input - how to isolate a hue? Include full range of sensor hues --> IR & UV!
  Extension: image upload

  1.2 Auditory output
  Generate tone - algorithm?

##2. See Sound
Auditory input (tone) -> visual output (hue)

  2.1 Auditory input
  Initially: live microphone input - how to isolate a tone?

  2.2 Visual output
  Generate hue - algorithm?

##3. Generator mode
One page: hue generator with readout linked to tone generator with readout - able to manipulate either affecting other.


\begin{description}
  \item[Hear Hue] Visual input (hue) \textrightarrow auditory output (tone)
  \item [See Sound] Auditory input (tone) \textrightarrow visual output (hue)
  \item [Generator]
About
Settings

------------------------


# 2016-10-13 Thr 14:52
Looking at creating an online repository so I can manage versions.

# 2016-10-13 Thr 13:28
Created a [GitHub repository](https://github.com/dvmansueto/HearHues).
...And confused myself at the command line trying to add an existing project, deleted the repo and made it again...
Success!
## Commit:
  >#1. First project commit

# 2016-10-23 Thr 17:35
## Commit:
  >#2. Provided ToneGenerator

# 2016-10-13 Thr 17:35
## Commit:
  >#3. Merge remote-tracking branch 'origin/master'

# 2016-10-13 Thr 19:37
## Commit:
  >#4. Implemented conversion calculation between hue and tone and visa versa.

# 2016-10-13 Thr 22:37
## Commit:
  >#5. Added navigation drawer, sandbox, new icons

# 2016-10-13 Thr 23:11
## Commit:
  >#6. Tidy up icons

# 2016-10-14 Fri 02:07
## Commit:
  >#7. Convert to fragment-based interface.

# 2016-10-14 Fri 02:13
## Commit:
  >#8. Clean up fragment-based interface.

# 2016-10-19 Wed 19:09
## Commit:
  >#9.Convert Hear Hue from Activity to Fragment.

# 2016-10-20 Thu 17:38
## Commit:
  >#10. Hear Hue activity working.

# 2016-10-20 Thu 18:53
## Commit:
  >#11. Got SettingsFragment working

# 2016-10-20 Thu 21:03
## Commit:
  >#12. Tweaking Palette settings in HearHueFragment

# 2016-10-21 Fri 12:54
## Commit:
  >#13. Added Tone Generator preferences. Preference retrieval actually works.

# 2016-10-21 Fri 14:30
## Commit:
  >#14. Populated About fragment.

# 2016-10-21 Fri 16:46
## Commit:
  >#15. Fix preferences editor. Fix HearHue frequency calculation.

# 2016-10-23 Sun 17:25
## Commit:
  >#16. Add hue and note display to Hear Hue fragment. Some tidying.







--------------------------------------










# 2016-10-23 Sun 18:37
Investigating GPS integration. Idea is "Walk a Tune": user takes photos which are geo-tagged... Could just be sequential.
GPS-to-tone: derive a tone from location somehow. "Tread Tones".
User can sample the tone, choose to save (add to sequence).
Lat-long theramin? Choose starting point (origin) and specify field height and width. Lat position determines frequency, long amplitude. Tap to sample, option to save. Or just run...

# 2016-10-23 Sun 18:46
https://developer.android.com/guide/topics/location/strategies.html

# 2016-10-23 Sun 19:25
Playing around trying to get virtual device to work (for GPS testing). ADB doesn't seem to offer a pretend camera, so just changing the default fragment to avoid the issue...

# 2016-10-23 Sun 19:41
## Commit:
  >#17. Account for edge of array errors.

# 2016-10-23 Sun 20:00
[Google's GPS demo](https://developer.android.com/guide/topics/location/strategies.html)
[Better GPS demo](http://www.vogella.com/tutorials/AndroidLocationAPI/article.html)

# 2016-10-23 Sun 20:10
[Make an alert dialog to prompt user to enable GPS](https://www.tutorialspoint.com/android/android_alert_dialoges.htm)

# 2016-10-23 Sun 21:28
Wow, there's a lot on how to handle permissions for API 23 (6.0) onwards. Implemented [Google's Permission Request Example](https://developer.android.com/training/permissions/requesting.html) as a generic function in `Utils`.
Callback method resides in calling class, doesn't make sense to push it out to `Utils` (want to be able to handle results directly).

# 2016-10-23 Sun 21:42
[Open nav drawer on permission refusal](http://stackoverflow.com/a/17822591)

# 2016-10-23 Sun 22:36
Finally got permission request working... There was a logic error.

# 2016-10-23 Sun 22:55
Reshuffled location access code.
* GPS permission is requested in onResume,
* If approved, GPS setting is requested in permission callback, else network location permission is requested.
* If network is approved, we try and limp along, else we apologise and exit to nav drawer.

# 2016-10-23 Sun 23:06
Getting unwanted cyclic permission request behaviour, time for bed.

# 2016-10-24 Mon 08:26
Investigating cyclic permission request

# 2016-10-24 Mon 08:36
Seems to be because onResume just keeps running the request because there's nothing else to do.

# 2016-10-24 Mon 09:03
Permission error seems to be because there is no callback / callback handling.

# 2016-10-24 Mon 09:15
Looking in to the whole `shouldShowRequestPermissionRationale()` thing again. Maybe this is part of the issue? Hard to imagine it, sure it is just for good will toward the user, rather than functional necessity.
`Show an expanation to the user *asynchronously*`
And how does one do that?
[Maybe using `Toast`](http://stackoverflow.com/a/39665876)

# 2016-10-24 Mon 10:04
Been trying restructuring code to see if there was a flow error.
[This new permissions guide](https://inthecheesefactory.com/blog/things-you-need-to-know-about-android-m-permission-developer-edition/en) explains permission groups: if you get one, you get others in group as well.
This makes `ACCESS_FINE_LOCATION` vs `ACCESS_COURSE_LOCATION` irrelevant; either gives both, and I had a tiered approach.

# 2016-10-24 Mon 10:14
[Solution!](http://stackoverflow.com/a/32715419)
Call `FragmentCompat.requestPermissions()` _not_ `ActivityCompat.requestPermissions()`. Ah...
Now we get to the callback!

# 2016-10-24 Mon 10:21
This stuff really makes a mess of flow. If I have permission, I do something, if I don't, I ask for it, and end up at a callback, and do something else...

# 2016-10-24 Mon 10:34
Permission is working now...
Added a few state watching booleans.

# 2016-10-24 Mon 10:52
Added some of the functions from [Google's GPS demo](https://developer.android.com/guide/topics/location/strategies.html)

# 2016-10-24 Mon 11:22
Well past time to do other homework!
## Commit:
  >#18. Introduction of TreadTunes
  > * Created TreadTunesFragment
  > * Changed default fragment to TreadTunesFragment
  > * Added manifest permission for GPS
  > * Implemented runtime permission for GPS
  > Next step: create 'LocTone' object to provide interface between location and audio; update LocTone.setLocation() in location listener.

# 2016-10-27 Thu 21:45
LocTone: idea is to establish a datum, then use relative latitude and longitude to alter the amplitude and frequency (or visa versa) of a generated tone.

# 2016-10-27 Thu 22:08
So far so good... Now how do you convert a difference between two Degree, Minute, Second doubles into a distance?
[Oooh, with haversines!](http://www.movable-type.co.uk/scripts/latlong.html)
Actually, looking in `Location.java`, much of the work has been done in the (convoluted) `computeDistanceAndBearing` function (line 310).
...Except we are dealing with a very simple case, pure lat or long. It would be overkill to use these vector equations when a magnitude equation would do.
But we do need to account for curvature of the Earth... perhaps at a given lat -- for longitude it should be constant, right?
Arc length, then. Circumference C = 2πr, so length = CΦ = 2πrΦ

# 2016-10-27 Thu 22:42
Cool. Now for Earth radius as a function of latitude.
[Radius of the Earth § Geocentric radius](https://www.wikiwand.com/en/Earth_radius#/Geocentric_radius)
          / (a²cosφ)² + (b²sinφ)²
R(φ) =   / ----------------------
.       √  (a•cosφ)² + (b•sinφ)²

# 2016-10-27 Thu 23:30
LocTone is looking good, just need to produce notes. Most of the legwork has been done, in HueTone. Is it better to replicate much of the code in LocTone, or is it better to roll it in to one new object, or perhaps into ToneGenerator? Then we'd have really to work out that SharedPreferences listener...
## Commit:
  >#19. Preliminary LocTone implementation.

# 2016-10-28 Fri 09:15
Before getting on to making noises, let's work out how to draw it.
I'd like a set of axis, each marked with both distance and amplitude/frequency, and then a mark for new location, and perhaps for a short time on update, the old location.

Let's make a new 'DrawLoc' object that ties all the pieces together
Or... Do we push drawing properties into LocTune?
For the drawing, we want:
Abscissa - longitude: Distance and Frequency values
Ordinate - latitude: Distance and Amplitude values
Locations - old and new
Returning freq/amp values as scalars (currently) feeds in to this nicely.

# 2016-10-28 Fri 10:32
Want to play without needing to set GPS locations, so making the touch version first.
I think the neatest way would be to make the Tinker fragment tabbed; alternatively we could have 'Tinker with Hues' and 'Tinker with Locs' fragments in the place of Tinker.
[Tabbed fragments inside a fragment](http://stackoverflow.com/a/21605672)
## Commit:
  >#20. Converted TinkerFragment to a tab host fragment with 'Touch Hues' and 'Touch Tones' tabbed fragments.

# 2016-10-28 Fri 10:41
Now that's done, I think it might be just as neat to have both activities in a linearLayout on the one fragment...

Drawing, I'm guessing I can create a Canvas object and set a view to display it?
[Canvas and Drawables](https://developer.android.com/guide/topics/graphics/2d-graphics.html)
Really can't get the specified example anymore, back to GoogleUni...

[Extending View for 2D drawing](http://www.compiletimeerror.com/2013/09/introduction-to-2d-drawing-in-android.html#.WBKbc_p94UE)

# 2016-10-28 Fri 12:09
Canvas is chugging along, but it would be nice if there was a DIP (display-independent-pixel) size...
[Stack exchange has the answer](http://stackoverflow.com/a/2406790)

# 2016-10-28 Fri 12:43
Tried giving it a run, just drawing the axis and, as expected, trying to get dimensions from within the constructor is a no-no.
For now, just moving any affected code into `onSizeChanged()`
Everything's huge! Time for some debugging.
  >D/LocView:  width: 1440 height: 2112
  >D/LocView: mWidth: 4320mHeight: 6336
  >D/LocView: mDpi: 560.0

Perhaps that should be _divide_ by density...
  >D/LocView:  width: 1440 height: 2112
  >D/LocView: mWidth: 480mHeight: 704
  >D/LocView: mDpi: 560.0

That's the h/w sorted, but getting display density plainly isn't right...
  >D/LocView:   width: 1440  height: 2112
  >D/LocView:  mWidth: 480 mHeight: 704
  >D/LocView:    mDpi: 560.0
  >D/LocView: density: 3.5
  >D/LocView:    dDpi: 560.0
  >D/LocView: scaledD: 3.5
  >D/LocView:    xdpi: 560.0
  >D/LocView:    ydpi: 560.0

Of course, that's 'dots-per-inch' not 'display-independent-pixels'... My bad.
The 'dip' I'm after is a double-negative, 1 px is now 1 dp, because the w/h have been scaled. Progress...

Compiles now, and draws a set of axes... But near the top-left, and not to size, and the y-ticks are all wrong.

Oh, I wonder if I haven't take into account canvas origin. Maybe that's top left... Nope, just calculating float[] steps in the wrong way.

It's all very nice now, but looks about 3.5/th the size. So I should leave w/h alone, and create mDip = density, use that to scale everything up to screen DPI.

# 2016-10-28 Fri 13:20
##Commit:
  >#21. LocTone now drawing DPI scaled axes

...But I don't like the 'portrait' axes, and the simplest solution is to make the view half the screen height.
So will ditch the tabbed interface, and just have one Tinker fragment with a HueView above a LocView.

# 2016-10-28 Fri 13:44
Rejigged the layout for two views on one fragment, using [Layout Weight](https://developer.android.com/guide/topics/ui/layout/linear.html) to autofill height.
Now, how do we get touch to change things...

Looks like we can [override `onTouchEvent()`](https://examples.javacodegeeks.com/android/core/graphics/canvas-graphics/android-canvas-example/)
Just care about action_move, I guess.
...Then we throw the new 'location' to a custom listener?

Alright, the x y coords are relative to top/left of the view, so we can just convert that into amp and freq percentages.
Okay, you can drag 'out of bounds'. Better impose some limits...

Ticks are not aligned, think we should add half the stroke width as an offset.

Added a mIsPortrait boolean for easy checking which dimension is larger.
Set tick length by `TICK_WIDTH_PERCENT` of smallest dimension for visual consistency.
Ticks still don't quite line up, must be a mathematics issue as it appears to be a linearly increasing error, but not that important for now.
Left a TODO.

# 2016-10-28 Fri 14:35
Canvas is updating nicely, just need to make the listener.
...Or do I just need to add a ToneGenerator object...?
Oh, yes, I wanted the fragment to have both a ToneGenerator and LocTone object, thus the listener bit. Makes sense.
[Creating custom listeners](https://guides.codepath.com/android/Creating-Custom-Listeners)

# 2016-10-28 Fri 15:06
##Commit:
  >#22. LocView mostly complete, uses a listener interface to push amplitude and frequency to TinkerFragment.

# 2016-10-28 Fri 15:19
De-convolving the frequency range stuff from HueTone is going to be necessary.
I'm getting the 'heavily object-oriented' bit now.

# 2016-10-28 Fri 15:37
Well that wasn't so bad.
##Commit:
  >#23. Moved frequency related methods to new ScalarTone() class.

# 2016-10-28 Fri 15:40
Now we can have just one ScalarTone for the whole app, and somehow wrangle an OnSharedPreferencesChangeListnener action in on it.

# 2016-10-28 Fri 15:51
Did the same thing to ToneGenerator.
...Except trying to access the object from the parent activity returns a null object.
[Extend Application](http://stackoverflow.com/a/21810308)
[Best way to share data between activities](http://stackoverflow.com/a/4878259)
Add an app singleton, with accessors (pull in onActivityCreated) and mutators (push in onStop) for the global objects.

# 2016-10-28 Fri 16:23
Fatal exception! That's no fun.
[Add `android:name=".ApplicationSingleton"` to manifest](http://stackoverflow.com/a/1945297)

# 2016-10-28 Fri 16:33
SharedPrefs weren't being applied (were only being checked in HearHueFragment), now are being applied in MainActivity every time the nav drawer is opened... Very crude.

# 2016-10-28 Fri 16:46
Crashes during tone generation if location updates are too fast:
  >`java.lang.IllegalStateException: play() called on uninitialized AudioTrack.`

Perhaps pushing generation to an async task will stop the buffers being overwritten during the playback command...

# 2016-10-28 Fri 16:57
##Commit:
  >#24. Reorganised ToneGenerator and ScalarTone to be global objects.
  >Moved sharedPref check to MainActivity (need to redo)

# 2016-10-29 Sat 14:16
Plan today is to fix the audio overplay error, with two ideas:
  * reduce the buffer size and loop lots,
  * pass a copy of the buffer so changes don't wipe the one playing.

# 2016-10-29 Sat 14:28
Changed ToneGenerator.generateTone() to take a byte[] as a parameter rather than using the private field.

  >`java.lang.IllegalArgumentException: Invalid audio buffer size.`

Maybe because I changed the way the mSoundBytes[] was defined; it appeared that it wasn't being updated when mPlaybackSeconds changed.
First defined on construct, then updated by forced application of sharedPref, then was updating again inside TinkerFragment to reduce buffer length.
Oo! It was working, _properly_, freq changing by x pos, albeit briefly.
Back to:
  >`java.lang.IllegalStateException: play() called on uninitialized AudioTrack.`

Oh, I think the error was:
`mToneGenerator.setPlaybackSeconds( 200/1000);`
Both ints, so was evaluating to (int) 0, rather than (double) 0.2.
Let's test!
Yep, `mToneGenerator.setPlaybackSeconds( 200.0/1000.0);` works.
Still gets the uninit error if you move too quick, though.

# 2016-10-29 Sat 14:58
Huzzah! Trawling AudioTrack.java, I found write() can be static or stream, so changing to AudioTrack.MODE_STREAM magically fixed the problem. Sweet.
## Commit:
  >#25. Fixed ToneGenerator by setting AudioTrack.MODE_STREAM.

# 2016-10-29 Sat 16:27
Cleaned up ToneGenerator(), loads more comments now, more clearly named fields, rewritten the actual sample generation: now have amplitude ramp up and down code, maybe fix the 'popping' noises.
Added 'view watching' to TinkerFragment; idea is to optimise mToneGenerator for either HueView or LocView, depending on which one the user is interacting with.
No error messages, but there's no sound...

# 2016-10-29 Sat 16:44
Oh yeah, I commented out the startTone() bit to move to playContinuously.
What chaos will this cause?
Plays for a bit and stops.

# 2016-10-29 Sat 16:50
Replaced the commands in 'endOfPlaybackReached' with stopTune(), and moved the old commands into stopTune(), for consistent behaviour.

# 2016-10-29 Sat 16:58
No audio reminded me I wanted to implement a Mute function; done as a private boolean in ToneGenerator.

# 2016-10-29 Sat 17:28
ToneGenerator code is looking very neat now, but it's still broken!
Shoehorning old tone generation code back in...
Nope! Doesn't really start.
Must be playContinuously, doesn't allow overwrite despite streaming.

# 2016-10-29 Sat 18:13
Egads... So much going nowhere!
Currently, checking for view on first touch, if it's LocView, we start playing...
I reckon we just jam a play() on in there, neatness be damned.
Surprise surprise that fixed it.
But there's a lot of latency, could be all the debug logs?
Must want to be an asynchronous task!

# 2016-10-29 Sat 18:30
## Commit:
  >#26. Slightly less bad ToneGenerator (still popping, holds up main thread)

# 2016-10-29 Sat 18:32
[AsyncTask](https://developer.android.com/reference/android/os/AsyncTask.html)
Made a subclass asyncTask in ToneGenerator, play() executes it, stop() onCancelled's it.
And it kind of works...
`java.lang.IllegalStateException: Cannot execute task: the task is already running.`
Play is playing when it's playing, which it's not supposed to do.
Maybe the if check should be a while check?
Log like crazy!
Replaced my own mPlaying boolean with `mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING`, error still occurs.
Read the dev page a bit more closely, I was calling `mAsyncPlaybackTask.onCancelled();` not `mAsyncPlaybackTask.cancel( true);`
Error still arises, and logging shows that it's not running when it says it is.
Maybe AsyncTask is a one-shot?
Recreating the task after cancelling.
Well it's not crashing anymore, but it's not making noise, either.

# 2016-10-29 Sat 19:30
Lots of latency, think it's generating the tone.
Tried making two async task classes: one with just playback, and one that generated the tone then played.
IDE says not to set a generic AsyncTask to different subclasses of task, and guess what it didn't work.
Having one instance variable was desirable so I could just cancel whichever version was running.
Hmm, what about calling async playback task when async generate task reaches onPostExecute?
Okay, now do I make task a private variable, or do I make it a local one and just make heaps of the things? Let's try the latter first.
Okay, that's garbage, cancel can't interrupt audio playback.

# 2016-10-29 Sat 20:36
It seems you can do terrible things by making many async tasks.
And one mustn't cancel null tasks.
Cool, now it doesn't break, it just trashes my console.

# 2016-10-29 Sat 21:02
## Commit:
  >#27. Even less bad ToneGenerator (no longer holds up main thread, still pops)

Playing with it, it would be nice if volume control was logarithmic not linear.
Looking at AudioTrack.java, linear is what we've got.

# 2016-10-29 Sat 21:16
Tried getting ramping working nicely, but it sounds terrible. Just not going to bother with it.
## Commit:
  >#28. Machine gun version of ToneGenerator

Actually, that was a logic error; was ramping down the middle of the samples, not the end.
Fixed, and it works rather nicely!

# 2016-10-29 Sat 21:42
Bit of scratching on paper and playing around in MatLab, I think a simple approximation of logarithmic volume can be achieved using:
`logVol = 0.1 * Math.pow( 10, linVol);`
That kind of worked. A little more MatLab, now using
`y = 0.01 * Math.pow( 10.0, 2.0 * x)`
Works nicely.

# 2016-10-29 Sat 22:01
## Commit:
  >#29. Fixed popping on ToneGenerator. Logarithmic volume on ToneGenerator.

# 2016-10-29 Sat 22:03
Working on TreadToneFragment again.
May as well convert LocView to work here too; just need to disable touch input.

# 2016-10-29 Sat 22:11
## Commit:
  >#30. Added mTouchAllowed flag to LocView so it can be used to display GPS based locations without allowing touch input.

# 2016-10-29 Sat 22:12
At some point it might be worthwhile converting newCoord and oldCoord to an queue of coords, with finalised length.

# 2016-10-29 Sat 22:17
Made more sense to define LocView listener after touch is allowed, as the listener will never fire otherwise.
## Commit:
  >#31. Minor code reorganisation.

# 2016-10-30 Sun 00:05
Nearly two hours of coding with no entries... Yup.
## Commit:
  >#32. Working towards implementing TreadTune.
  > * Imported new icons for Tread Tune
  > * Played around with the layout XML, settled on Relative because it seems the neatest if a little finicky
  > * Updated ViewTone to use float[] for coordinate pairs rather than discrete floats
  > * Updated ViewTone to take scalar coordinates [0...1]
  > * Implemented an onClickListener in TreadTuneFragment
  > * Using boolean to flag if the next fix should reset the datum
  > * Flag set initially so first GPS fix is taken as initial datum

Tested it on phone, and it crashed. Cool!

# 2016-10-30 Sun 00:31
Just been covering code with debug logs...
Was down to checking if LocTone.deltaToScalar was working, but now not sure if TreadTuneFragment.onClick is actually working.

# 2016-10-30 Sun 00:42
Okay, I had I typo in deltaToScalar (not catching 0 despite comments saying I was...)
Now I'm thinking that the way I'm updating LocView (creating _new_ arrays) onDraw is never being called, however it is that it's drawn...

# 2016-10-30 Sun 00:50
Setting bogus primitives to draw didn't prompt onDraw, must be something else, and onTouchEvent was one such else.
[The solution is `invalidate()`](http://cogitolearning.co.uk/?p=1663)
...Wasn't actually the solution. View.java:
  >Invalidate the whole view. If the view is visible,
  >{@link #onDraw(android.graphics.Canvas)} will be called at some point in
  >the future.

And that "some point" is when Android thinks something has changed, which it plainly doesn't.

# 2016-10-30 Sun 01:13
It was working; I made a mathematical error in newScalarCoord.
But calling invalidate was necessary.

# 2016-10-30 Sun 01:21
Not getting any sound, but it does work in TinkerFragment, and then _briefly_ after swapping back to TreadTuneFragment.

# 2016-10-30 Sun 01:43
ToneGenerator wasn't being initialised until the first time the nav drawer shut.

# 2016-10-30 Sun 01:50
## Commit:
  >#33. TreadTune working.
  > * Forced onDraw call on coord update.
  > * SharedPrefs updated in MainActivity onCreate

# 2016-10-30 Sun 08:44
Changing private field javadoc comments to one line in ToneGenerator.
Maybe that looks better? Certainly more condensed!

Noticed `AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE`, might be nice to use that.
Can't apply?
It's a preference key, need to call `AudioManager.getProperty( AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)` but can't from static context...
[Not worth the effort, just set to 44100](https://developer.android.com/reference/android/media/AudioManager.html)
Sounds better now too, fancy that! (Was 9600)

Playing with linToLog, works better with a little more power.
Think I have a relationship for the variables, so can change to a sharedPref implementation.
Created preference, applied in MainActivity.
Hmm, sounds good more aggressive. Adding more aggressive options.
And more! Started with [1...4], now have [1...7], default 5 (first implementation was 2!).

# 2016-10-30 Sun 10:18
## Commit:
  >#34. Implemented sharedPref for ToneGenerator.linToLog(). Some minor tidying.

# 2016-10-30 Sun 10:21
Would like `back` from settings to return to last fragment, and have the mute feature work.

Mute implemented; was a little silly to change drawable:
`muteButton.setIcon( getResources().getDrawable( R.drawable.ic_volume_unmuted));`, but it's working, and with the images the right way around, now, too!

# 2016-10-30 Sun 10:39
## Commit:
  >#35. Implemented ToneGenerator muting.

# 2016-10-30 Sun 10:41
On to: [Back Navigation](https://developer.android.com/training/implementing-navigation/temporal.html)
Because I'd like to get the API level down to 15, also changing to Support Fragments while we're at it.

# 2016-10-30 Sun 12:08
Thought we were nearly there, but back is adding fragments, not replacing.
Adding seems to happen forwards too, after a few fragments have changed.
Does it happen without adding to the backstack?
Yep, it looks like it was happening before but was never tested for.
...But not on older version on my phone.
[Define background in XML](http://stackoverflow.com/a/28686712)

# 2016-10-30 Sun 13:03
## Commit:
  >#36. Implemented Back functionality.

## Commit:
  >#37. Disabled LocView touch input in TreadTune

# 2016-10-30 Sun 13:37
Added more info to About, made About scrollable, changed name of TreadTune to TreadTone.
## Commit:
  >#38. Tidying, extended About.

# 2016-10-30 Sun 16:48
Decided to pull the Camera2 stuff out of HearHueFragment because it was such a mess I couldn't find anything.
Teased it apart and pushed relevant bits into new CamTone.java (although it has no 'tone' aspect).
What a pain! Should have been logging...
But it's done.
Discovered that Toast causes 'screen overlay' issues on permission dialogs, so they're gone.
Had cyclic permission request, so I put the flags back.
Capture from front camera still doesn't work.
## Commit:
  >#39. Cleaning up HearHueFragment

# 2016-10-30 Sun 17:26
Trying to display the current tone as Hz / note on TreadTone, apparently mFrequency is always null even after just being set..
## Commit:
  >#40. Working towards displaying tone as string on Tread Tone

# 2016-10-31 Mon 15:21
Okay, making sure TreadTone really works and we'll call it.

# 2016-10-31 Mon 15:46
Duuuuh...
`java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String net.dvmansueto.hearhues.ScalarTone.toNoteString(double)' on a null object reference`
It wasn't mFrequency which was the null object, it was mScalarTone!
Since tones can change, I wanted to pass 'the' ScalarTone for the app, but that's probably not the best idea.
Better to have TreadToneFragment take the frequency from LocTone and pass that as an argument to it's already-sourced ScalarTone.
Still messy, I think in one way it would be neater to have it done black box style inside LocTone, but then we have to keep it's version of 'the' ScalarTone up-to-date.
...Could have ScalarTone act as a 'Utils' class, and ensure it keeps itself up-to-date...
Would have to check it's up-to-date every time it was accessed, that's very slow.
Could do a half-way; listener in main activity, changes a global boolean to say there's a change... May as well change the global ScalarTone.
[Finally implementing a onSharePreferenceChangeListener...](https://developer.android.com/guide/topics/ui/settings.html)

# 2016-10-31 Mon 16:32
SharedPref listener is go. Now, if I call a method of the class, will it be updated... Yeah, still don't think this is going to work.
No, because it relies upon internal variables that need to be updated.

For HueTone, I pass a ScalarTone in the constructor.
I don't really like that, but I like the 'black box' return.
ScalarTone is used quite a bit inside HueTone.
And it's never updated; HueTone is constructed anew each onResume, though.

Tried to test; opening HueTone in simulator still crashes, so fixing that.
`java.lang.IllegalArgumentException: Camera id null does not match any currently connected camera device`
Made findCameraIds() return whether it found an ID, so we can say:
```java
if ( mCameraId == null) {
  if ( !findCameraIds()) return;
}
```
# 2016-10-31 Mon 17:16
Reorganised HearHue a bit, it was rather untidy.
Interesting, ScalarTone now really provides the initial premise of HueTone, interfacing between hues and tones. Now one is defined using a ScalarTone method of the other.

LocTone: we don't need to update local location variable and then retrieve freq/amp; we can just do that in one method.

# 2016-10-31 Mon 19:35
So many comments.

# 2016-10-31 Mon 20:22
So many commits.
  >#41. Renamed ApplicationSingleton to ApplicationContext since it's called by `getApplicationContext()`

  >#42. Fix opening null camera ID error: made `findCameraIds()` boolean, returns true only if ID not null.

  >#43. Clean up after isolating from ScalarTone.

  >#44. Clean up after isolating out CamTone.

  >#45. Substantial re-write, now takes a datum and then provides relative utility functions.

  >#46. Implements onSharedPreferenceChangeListener. Some tidying.

  >#47. Tidying and minor tweaks (robustness).

  >#48. Tidying. Adapt to modified LocView.

  >#49. Added playStop() method.

  >#50. setOnClickListener for 'origin' button, adapted to suit modified LocTone, implemented frequency/amplitude text display, added sharedPreferences for window height & width and location timeout, tidying.

  >#51. Minor code cleanup.

# 2016-10-31 Mon 21:32
That's the app dev part 99% done as far as the assignment is concerned!
Still remaining, dev wise:
  * Cosmetics (themes, night mode)
  * Add a HueView to Tinker (HSL plot, hue callback on touch feeds tone gen)
  * Add a See Sound activity: use microphones to capture audio, run though FFT and convert the most powerful frequency to a colour to display.
  * PrefsFrag: Show current setting as summary
  * Add axis labels to LocView
  * Push minimum APK down to 15
  * Fix AppBar elevation
  * Create a landing page (just the nav draw, really, but so exit on back press is expected)
  * Fix capture from front camera

# 2016-11-01 Tue 08:16
[Fixed toolbar elevation](http://stackoverflow.com/a/30806515)
## Commit
  >#52. Create toolbar elevation effect for API < 21.

  >#53. Adapt for new toolbar layout

# 2016-11-01 Tue 10:15
Trying to fix the fragment over fragment issue; replaced fragments still visible and clickable.
Changed method to only add different fragments to backstack; changing from Frag A to Frag B to Frag A still makes new instance of Frag A and adds to backstack; would prefer returned to Frag A, but then how do you keep Frag B behind it on backstack?
Old fragments remaining clickable is fixed in XML layout of each fragment:
  * [`:clickable="true"`](http://stackoverflow.com/a/23729005)

# 2016-11-01 Tue 11:16
Didn't update the commit message before committing, so got to play with `git commit --amend`; and a wild vim appeared!
## Commit:
  >#55. More rigorous fragment replacement

Making a 'Landing Page' fragment.
How to actually implement the 'onClick'... Don't want to copy NavDraw method, but don't want to rely upon it either.
Alright, since we're instantiating LandingFragment anyway, made a listener for it, which MainActivity implements.
## Commit:
  >#56. Implemented a LandingFragment

# 2016-11-01 Tue 13:51
## Commit:
  >#57. Rationalised icons. Added fragment icons to action bar.

  >#58. Updated Tinker headings.

  >#59. Minor logic flow revision

  >#60. Fixed HearHueFragment not playing subsequent tones by forcing mAudioTrack.stop() when end of buffer marker reached.

# 2016-11-12 Sat 12:48
Got an extension, so better polish things up!
Tread Tone activity only kind of works; locations are retrieved and sounds are made, but there is a lot of 'drift' on location, and there can be big jumps even when moving at steady pace. Experience playing Ingress leads me to believe this is simply how smartphone location awareness is.

To combat drift, it makes sense to reduce the 'sensitivity', i.e. enlarging the window, so error (unchanged) is reduced compared to step (larger).

Currently, based on thinking accuracy can't be as good as 1 m but is probably better than 10 m, our 'ready-reckoning' options are:

```XML
  <item>10</item>
  <item>25</item>
  <item>50</item>
  <item>75</item>
  <item>100</item>
  <item>200</item>
```

Which, divided by 12 (window height & width in steps) give step sizes in metres:

```Matlab
>> [ 10 25 50 75 100 200] / 12
ans =
    0.8333    2.0833    4.1667    6.2500    8.3333   16.6667
```

But how accurate is the GPS, or, what size should the step size really be?

Looking for articles on accuracy of smartphone GPS sensors.
(Actually spent more time playing with referencing than searching...)

[Zandbergen and Barbeau (2011)](#Zandbergen2011) found a mean error of 5.0 m using a Sanyo SCP-7050 GPS-equipped phone, while [Garnet and Stewart (2015)](#Garnet2015) calculate the mean static GPS location error for an iPhone 2 as 3.89 m in open areas and 11.2 m near buildings 2 to 8 storeys high: this is pretty much what I estimated.

My own experience is that Google has put considerable effort into improving location accuracy, such as using cellular triangulation and WiFi MAC/SSID sniffing, so we can assume modern Android phones achieve at least as good accuracy as the iPhone 2.

Lower accuracy is desirable, as playing fun tunes requires rapidly changing coordinate, which in turn is a function of real-world movement (walking speed) and the 'step size' of the Tread Tune window. At the same time, drift and jumps are annoying, especially where they simply push you against the window boundary.

Since the literature basically supports our estimate, we can try increase the number of options and play around and see what happens.

9 options ranging logarithmically from unrealistically small to aerobically large:

```Matlab
>> logspace( log10(1), log10(20), 9)
ans =
  Columns 1 through 6
    1.0000    1.4542    2.1147    3.0753    4.4721    6.5034
  Columns 7 through 9
    9.4574   13.7531   20.0000
>> ans * 12
ans =
  Columns 1 through 6
   12.0000   17.4506   25.3769   36.9035   53.6656   78.0414
  Columns 7 through 9
  113.4890  165.0374  240.0000
```

```XML
  <item>12</item>
  <item>17</item>
  <item>25</item>
  <item>37</item>
  <item>54</item>
  <item>78</item>
  <item>113</item>
  <item>165</item>
  <item>240</item>
```

# 2016-11-12 Sat 14:51
Tested new GPS options and either I'm getting location errors as much as 120 m, or I'm not actually applying the window size preferences properly...

Adding some debug logging...

Wanted to see location on the screen, so re-jigging the layout so both location and tone strings are displayed.

Okay, now I want to see both location and relative coordinates!
Adding a `String getLocText()` method to `LocTone` to do all the heavy lifting.
Or am I? Putting it inside requires LocTone to have memory; at the moment it's basically a utility class, but then it remembers a datum/origin point.
I guess if it has any memory there's no problem adding more.
But should I change the way it works and pass only a new `Location` then use accessors for everything? That would be consistent.


# 2016-11-12 Sat 15:58
## Commit:
  >#61. Updated `LocTone`:
  >  * Replaced origin lat/long doubles with a Location
  >  * Added memory of current Location
  >  * Added `setLocation()`
  >  * Converted `double latitudeToScalar( double latitude)` to `double getScalarLatitude()`, similarly for longitude
  >  * Added `toDegreeString()` and `toCoordString()`

`TreadToneFragment.java` either sets a new datum or a new location on every new good location.

# 2016-11-12 Sat 16:15
Wow, does this thing whip around!

The loc string is too long, and disappears under the 'set datum' icon.
Really, the icon is in a silly place, could just move it away. Where to put it, though? Action bar? Kind of hidden. Floating? Obscures main view. Leave it where it is!

Anyway, I wonder if there is a way we can improve accuracy.
[Accuracy is the **radius** of 68% (2σ) confidence](https://developer.android.com/reference/android/location/Location.html)

Can't simulate accuracy, and can't get real locations indoors...
Add a preference and play around!
Google sample code uses 200 as 'significantly less accurate', so we could choose static values:

```Matlab
>> logspace( log10( 10), log10( 200), 7)
ans =
  Columns 1 through 6
   10.0000   16.4755   27.1442   44.7214   73.6806  121.3924
  Column 7
  200.0000
```

Or we could use proportional values, say percent of window width:

```Matlab
>> logspace( log10( 10), log10( 100), 7)
ans =
  Columns 1 through 6
   10.0000   14.6780   21.5443   31.6228   46.4159   68.1292
  Column 7
  100.0000
```

Let's start with static values and see what happens. If things seems somewhat logical, move to proportional values.

Doesn't do a thing.

I wonder if the issue arises from how the difference between a new location and the origin is calculated:

```Java
// convert angular separation from degrees to radians
double angle = (latitude - mOrigin.getLatitude()) * Math.PI / 180.0;

// arc length = angle * ( 2 * PI * radius)
return 2.0 * Math.PI * mLatitudinalRadius * angle;
```

Makes sense, seems to work, but haven't tested it properly.





# References

<a name="Zandbergen2011">Zandbergen, P.A. and Barbeau, S.J., 2011. Positional accuracy of assisted gps data from high-sensitivity gps-enabled mobile phones. Journal of Navigation, 64(03), pp.381-399. DOI:[10.1017/S0373463311000051](http://dx.doi.org/10.1017/S0373463311000051)</a>

<a name="Garnet2015">Garnett, R. and Stewart, R., 2015. Comparison of GPS units and mobile Apple GPS capabilities in an urban landscape. Cartography and Geographic Information Science, 42(1), pp.1-8. DOI:[10.1080/15230406.2014.974074](http://dx.doi.org/10.1080/15230406.2014.974074)</a>