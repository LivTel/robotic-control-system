#!/bin/csh
#
# Makes 
#
# Usage: make_fits.csh <fits-file> <compress-code> <output-file>
#
set log = /occ/tmp/fits.log

echo >>!  $log
echo "--------------------------------------------------------" >>!  $log
date -u >>!  $log
echo " Make fits with: " $* >> $log

if (${#argv} != 3) then
    echo "Missing args only ${#argv} not 3" >> $log
    exit 1
endif
echo "Got 2 args" >> $log

# Check all files exist.
if (! -e $1 ) then
    echo "input FITS file is missing:" >> $log
    exit 2
endif
echo "OK input fits" >> $log

# Check the compress arg.
if ($2 < 100 || $2 > 900) then
    echo "Compress parameter $2 out of range:" >> $log
    exit 3
endif
echo "OK compression" >> $log

# Check that output does not already exist.
if (-e $3) then
    echo "local output file $3 already exists: " >> $log
    exit 4
endif
echo "OK output" >> $log

# Ok so far...
echo "Calling squash" >> $log

/occ/util/squash $1 $2 >>&! $log

# Copy the remote file to local and delete remote (NFS).
set out = ${1}.H
if (-e $out) then
    # Copy.
    cp $out $3
    if ($status == 0) then
	echo "Copied remote file to local"  >> $log
    else
	echo "Failed to copy remote file to local"  >> $log
    endif
    # Remove.
    rm -f $out
    if ($status == 0) then
	echo "Deleted remote file" >> $log
    else
	echo "Failed to delete remote file" >> $log
    endif
else
    echo "No output file was generated" >> $log
    exit 5
endif




