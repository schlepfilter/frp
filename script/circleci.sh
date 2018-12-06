#!/usr/bin/env bash
git clone -b develop https://github.com/schlepfilter/aid &&
cd aid &&
lein install &&
cd .. &&
lein test &&
lein npm install &&
lein doo chrome test once
