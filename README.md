V Android Wear watch face Lite version
=================================

This is an Analogue Watch Face for Wear OS, Lite version.

As the watch face version, I made for Facer was consuming too much power
I decided to do a standalone version of it.
Thanks to this I managed to save around 30% of battery power
comparing to use this watch face with Facer.
This project is mainly based on watch face codelab (https://watchface-codelab.appspot.com).
However, I made some significant changes to this project, like:

For an aesthetic reason, I used graphics instead of drawn watch hands, but to preserve more power,
I used graphics with as lees of transparency as it was possible.
Also, graphics used in ambient mode are purely black and white which helps preserve even more power
and prevent burn-in effect on OLED screens.

I also have to figure out how to make a battery gauge represent a current power level of the watch.
To calculate an angle for rotation of the battery hand to represent an actual level of the battery
I made below assumptions:

* As the battery hand make half of the circle, the range for it is 180 degrees
  (-90 to +90 with 0 in the half point).
* Battery charge range is from 0-100, hence 180 / 100 = 1.8.
* So, each 1% of the battery correspond to 1.8 degrees.
* So, to calculate battery hand angle, the value of the battery charge level needs to be multiplied by 1.8,
  but as the battery hand range is from -90 to 90, from the above result it needs to be deducted 90 degrees.
* For example:
* For 0% charge: 0*1.8 = 0, 0-90 = -90 (hand will go to bottom left on the scale)
* For 50% charge: 50*1,8 = 90, 90-90 = 0 (hand will go to the centre of the scale)
* For 100% charge: 100*1,8 = 180, 180-90 = 90 (hand will go to the bottom right of the scale).

Patches are encouraged and may be submitted by forking this project and
submitting a pull request through GitHub.

Happy codding ;)

License
-------

Copyright 2016 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
