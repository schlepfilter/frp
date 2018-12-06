#TODO rewrite this script in closh when closh becomes stable
#!/usr/bin/env bash
git clone -b develop https://github.com/schlepfilter/aid &&
cd aid &&
lein install &&
cd .. &&
lein test &&
lein npm install &&
#TODO test other environments
lein doo chrome-headless test once &&
lein cljsbuild once prod
