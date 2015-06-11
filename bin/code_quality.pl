#!/usr/bin/perl

use strict;


open FILE, "plugins/idea/betterdocsidea/target/findbugs/report.xml" or die "Couldn't open file: $!"; 

my $string = "";
my $count = -1;

# Findbugs
while (<FILE>) {
    $string .= $_;
    if(/<Errors errors=\"(\d+).*/) {
        $count = $1;
    }
}
if ($count > 0) {
    print "ERROR: Found findbug violations.!!\n";
    print $string;
    exit(1);
}
print "SUCCESS: No findbug violations\n";
close FILE;

# Checkstyle
open FILE, "plugins/idea/betterdocsidea/target/checkstyle-result.xml" or die "Couldn't open file: $!"; 

my $string = "";
my $count = 0;

while (<FILE>) {

    if(/<file .*/) {
        $string .= $_;
    }

    if(/<error .*/) {
        $string .= $_;
        $count = $count + 1;
    }
}

if ($count > 0) {
    print "ERROR: Found checkstyle violations.!!\n";
    print $string;
    exit(1);
}

print "SUCCESS: No checkstyle violations\n";
close FILE;

# PMD
open FILE, "plugins/idea/betterdocsidea/target/pmd.xml" or die "Couldn't open file: $!"; 

my $string = "";
my $count = 0;

while (<FILE>) {

    if(/<file .*/) {
        $string .= $_;
    }

    if(/<violation .*/) {
        $string .= $_;
        $count = $count + 1;
    }
}

if ($count > 5) { # allowing 5 violations
    print "ERROR: Your code adds PMD violations.!!\n";
    print $string;
    exit(1);
}

print "SUCCESS: No PMD violations\n";
close FILE;
