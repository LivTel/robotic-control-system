#!/bin/csh
#
# Makes a Color JPEG from 3 color-filtered FITS images.
#
# Usage: make_jpeg.csh <input-fits-file> <output-jpeg-file> <type-code> <config>
#

set log = /occ/tmp/jpeg.log
set t1 = `date -u "+%s"`
echo >>!  $log
echo "--------------------------------------------------------" >>!  $log
date -u >>!  $log
echo " Make greyscale with: " $* >> $log
echo "call make_jpeg with : " $* 

if (${#argv} != 4) then
    echo "Missing args only ${#argv} not 4" >> $log
    exit 21
endif
echo "got 3 args" >> $log

# Check all files exist.
if (! -e $1 ) then
    echo "fits file is missing:" >> $log
    exit 22
endif
echo "Ok fits" >> $log


## Try to create output file.
#touch $2
#set ss = $status
#if ($ss != 0) then
 #   echo "Cannot create output file $2 status: $ss" >> $log
  #  exit 5
#endif
#echo "OK output" >> $log

# Now loose it.
#rm -f $2

set code = $3
#@ code = $code - 690500
echo "try code $code"

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

# Check the type.
#if ($code < 0 || $code > 2) then
 #   echo "Type not 0-2: " $code  >> $log
  #  exit 6
#end

# Ok so far...
echo "Calling gpp" >> $log

/occ/util/gpp $1 $2 $mycode /occ/misc/config/gpp.cfg >>&! $log

set ex = $status

echo "Completed greyscale with exit status $ex" >>&! $log

set t2 = `date -u "+%s"`
echo "Done at $t2 " >> $log
set dd = 0
@ dd = $t2 - $t1
echo "Took altogether $dd secs" >> $log

exit $ex
