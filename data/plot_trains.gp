#!/usr/bin/env gnuplot
# This script will create line graphs for all of the passenger data

# Use first row as column names
set key autotitle columnhead
set ylabel 'Number of Passengers'
set yrange [0:170]
set xlabel 'Stations Visited'
set grid
set key off
set term png

output_dir = './gnuplotted'
files = system('ls *train.csv')
system 'mkdir -p ' . output_dir

do for [file in files] {
	set output sprintf(output_dir . '/%s.png', file)
	# We don't enhance the title in order to prevent underscores from being
	# interpreted for subscripts.
	set title sprintf('%s', file) noenhanced
	plot file with lines
}

print 'Graphs successfully created in: ' . output_dir
