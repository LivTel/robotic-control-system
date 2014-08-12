#!/bin/csh
#
# Makes 
#
# Usage: make_mosaic_fits.csh <n-fits-file-list> <output-file>
#
set log = /occ/tmp/fits.log
set t1 = `date -u "+%s"`

echo 
#>>!  $log
echo "--------------------------------------------------------" >>&!  $log
date -u >> $log
echo " Make mosaic fits with: " $* >>& $log

if (${#argv} < 2) then
    echo "Missing args only ${#argv} not 2+" >>& $log
    exit 1
endif

echo "Got ${#argv} args" >>& $log

set na = ${#argv}
@ na = $na - 1

# Check all files exist.
set ii = 1
while ($ii <= $na)  
    if (! -e $argv[$ii]) then 
	echo "Input FITS file $ii of $na is missing:" >>& $log
	exit 2
    endif
    @ ii++
end
echo "OK $na input fits files" >>& $log

# Check that output does not already exist.
set la = $na
@ la = $la + 1
set ofile = $argv[$la]
if (-e $ofile ) then
    echo "Local output file $ofile already exists: " >>& $log
    exit 3
endif
echo "OK output file " >>& $log

# Ok so far...

set al = ""
set ii = 1
echo "Adding $na input files...." >>& $log
while ($ii <= $na)  
    echo Adding $ii $argv[$ii] >>& $log
    set al = `echo ${al}`" -i "$argv[$ii]
    @ ii++
end

set temp = "/occ/rcs/planetarium/images/tmp.fits"
set al = `echo ${al}`" -o "${temp}

echo "Calling mos20b using: $al " >>& $log

/occ/util/mos20b $al >>& $log

set ex = $status

echo "Completed mosaic-fits processing with status $ex " >>& $log

if ($ex == 0) then
    set mycode = 1
    echo "Calling gpp4 using: $temp $ofile $mycode "  >>& $log

    /occ/util/gpp4 $temp $ofile $mycode >>& $log

    set ex = $status

    echo "Completed mosaic-jpeg processing with status $ex " >>& $log

endif

set t2 = `date -u "+%s"`
echo "Done at $t2 " >>& $log
set dd = 0
@ dd = $t2 - $t1
echo "Took altogether $dd secs" >>& $log

exit $ex




