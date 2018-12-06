#!/usr/bin/env bash
git clone -b develop https://github.com/schlepfilter/aid &&
cd aid &&
lein install &&
cd .. &&
lein test
