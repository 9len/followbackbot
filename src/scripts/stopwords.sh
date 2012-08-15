#! /bin/sh

for WORD in `egrep "ing$" /usr/share/dict/words`; do
  LEGIT=`wn $WORD | grep "Information available for verb"`
  if [ -z "$LEGIT" ]; then
    echo "    \"$WORD\"," | tr '[A-Z]' '[a-z]'
  fi
done
