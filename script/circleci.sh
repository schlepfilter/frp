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
lein install &&
cd examples &&
rm -rf resources/public &&
git clone "https://schlepfilter:$GITHUB_TOKEN@github.com/frpexamples/frpexamples.github.io.git" resources/public &&
git checkout -- . &&
lein clean &&
lein cljsbuild once prod &&
cd resources/public &&
git add . &&
git config --global user.email "you@example.com" &&
git config --global user.name "Your Name" &&
git commit -m $CIRCLE_SHA1 | grep "changed" &&
git push origin master
