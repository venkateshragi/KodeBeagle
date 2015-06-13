#!/usr/bin/perl

use strict;


open FILE, "plugins/idea/kodebeagleidea/src/main/resources/META-INF/plugin.xml" or die "Couldn't open file: $!"; 

my $string = "";
my $count = 0;

while (<FILE>) {

    if(/version/) {
        $string .= $_;
    }

    if(/VERSION_/) {
        $count = $count + 1;
    }
}

if ($count < 2) {
    print "ERROR: Your patch changes plugin.xml and removes important VERSION_ placeholders.!!\n";
    print $string;
    exit(1);
}

close FILE;

print "SUCCESS: PR validations.\n";
