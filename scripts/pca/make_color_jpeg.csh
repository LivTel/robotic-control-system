#!/bin/csh
#
# Makes a Color JPEG from 3 color-filtered FITS images.
#
# Usage: make_color_jpeg.csh <red-file> <green-file> <blue-file> <output-file> <type-code>
#

set log = /occ/tmp/jpeg.log
set t1 = `date -u "+%s"`
echo >>!  $log
echo "--------------------------------------------------------" >>!  $log
date -u >>!  $log
echo " Make color with: " $* >> $log
echo "call make_color with : " $* 
echo "Start at $t1 " >> $log
if (${#argv} != 5) then
    echo "Missing args only ${#argv} not 5" >> $log
    exit 21
endif
echo "got 5 args" >> $log

# Check all files exist.

if (! -e $1 ) then
    echo "red file is missing:" >> $log
    exit 22
endif
echo "Ok red" >> $log

if (! -e $2 ) then
    echo "green file is missing:" >> $log
    exit 23
endif
echo "OK green" >> $log

if (! -e $3 ) then
    echo "blue file is missing:" >> $log
    exit 24
endif
echo "OK blue" >> $log

## Try to create output file.
#touch $4
#set ss = $status
#if ($ss != 0) then
 #   echo "Cannot create output file $4 status: $ss" >> $log
  #  exit 5
#endif
#echo "OK output" >> $log

# Now loose it.
#rm -f $4

set code = $5
#@ code = $code - 690510

switch ($code)
    case 690508:
    set mycode = 0
    breaksw
    case 690509:
    set mycode = 2
    breaksw
    case 690510:
    set mycode = 1
    breaksw
    default:
    set mycode = 1
    breaksw
endsw

echo "Code altered to $mycode" >> $log

set code = 1


# Check the type.
#if ($code < 0 || $code > 2) then
 #   echo "Type not 0-2: " $code  >> $log
  #  exit 6
#end

echo "using code $mycode " >> $log

# Ok so far...
set tx = `date -u "+%s"`
echo "Calling ppp at $tx" >> $log

# First record which version were running
/occ/util/ppp -V >>&! $log
/occ/util/ppp -r $1 -g $2 -b $3 -o $4 -m $mycode -c /occ/misc/config/ppp.cfg  >>&! $log

set ex = $status

echo "Completed color-jpeg processing with status $ex " >> $log

set t2 = `date -u "+%s"`
echo "Done at $t2 " >> $log
set dd = 0
@ dd = $t2 - $t1
echo "Took altogether $dd secs" >> $log

exit $ex
