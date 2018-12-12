#!/usr/bin/env bash
cd examples/resources/public &&
git add . &&
git config --global user.email "you@example.com" &&
git config --global user.name "Your Name" &&
if git commit -m $CIRCLE_SHA1 | grep "changed"; then
  git push origin master
fi
