#!/usr/bin/perl

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

use strict;

# uncomment the following to run it.
#my $file = "collated";
my $temp_file = "result_list";

#my $pass = "<>";

#my $output_dir="<output-dir>";

# All the hosts.
my $host26="pramati\@192.168.2.26";
my $host27="pramati\@192.168.2.27";
my $host67="pramati\@192.168.2.67";
my $host157="hadoop-user\@192.168.2.157";

print $host26."\n";
open my $input, '<:encoding(UTF-8)', $file or die "can't open $file: $!";

my %map_repo_id;

while (<$input>) {
    chomp;
    if(/repo~.*?~.*?~(\d+?)~.*~(\d+?)\.zip/) {
        if($2 > 1) {
            $map_repo_id{$1}=$_; # This will also remove duplicate entries.
        }
    }
}

#print $map_repo_id{"14771504"}."\n";
print "Found ", scalar(keys(%map_repo_id)), " entries matching the criteria.\n";
close $input or die "can't close $file: $!";

open my $output, '>:encoding(UTF-8)', $temp_file or die "can't open $file: $!";

foreach my $repo_file_name (values %map_repo_id) {
    print $output "$repo_file_name\n";
}

close $output or die "can't close $file: $!";
# Copy from all the location to one place. 
`sshpass -p $pass rsync -avi -e ssh --files-from=$temp_file $host26:/mnt/betterdocs_data/github2/ $output_dir 2>/dev/null`;
`sshpass -p $pass rsync -avi -e ssh --files-from=$temp_file $host26:/mnt/betterdocs_data/github4/ $output_dir 2>/dev/null`;
`sshpass -p $pass rsync -avi -e ssh --files-from=$temp_file $host27:/mnt/betterdocs_data/github27/ $output_dir 2>/dev/null`;
`sshpass -p $pass rsync -avi -e ssh --files-from=$temp_file $host157:/mnt/betterdocs_data/github157/ $output_dir 2>/dev/null`;
`sshpass -p $pass rsync -avi -e ssh --files-from=$temp_file $host157:/mnt/betterdocs_data/github4/ $output_dir 2>/dev/null`;
`sshpass -p $pass rsync -avi -e ssh --files-from=$temp_file $host67:/mnt/betterdocs_data/github4/ $output_dir 2>/dev/null`;
`sshpass -p $pass rsync -avi -e ssh --files-from=$temp_file $host67:/mnt/betterdocs_data/github67/ $output_dir 2>/dev/null`;
