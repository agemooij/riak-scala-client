#!/bin/sh -ex

DIR=$(basename $0 | xargs dirname)
TMP=$DIR/tmp
RIAK=$TMP/riak-2.0.2/rel/riak

(
    cd $RIAK
    bin/riak start
)