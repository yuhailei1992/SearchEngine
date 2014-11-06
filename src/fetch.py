import os.path,subprocess
from subprocess import STDOUT,PIPE
import re
import sys
import fileinput

def execute_java(java_file, input):
	java_class,ext = os.path.splitext(java_file)
	print java_class
	cmd = ['java', '-Xms250m', '-Xmx2048m', '-cp', '.:lucene-4.3.0/*', java_class, input]
	proc = subprocess.Popen(cmd)
	stdout,stderr = proc.communicate()

def writeFile(text):
	with open("Exp4.txt", "a") as myfile:
		myfile.write(text + "\n")

fileIn = '/Users/Caesar/Documents/workspace/SearchEngineLab1/HW1-queries-UB.teIn'

baseline = {'10':0.0170, '12':0.2721, '26':0.0340,
		    '29':0.0165, '33':0.3836, '52':0.0615,
		    '71':0.0344, '102':0.0007,
		    '149':0.1168, '190':0.0432}
MAPPattern = re.compile(r'^(?i)MAP\s+\d+')
MAPAllPattern = re.compile(r'^(?i)MAP.*all.*')
P10AllPattern = re.compile(r'^(?i)P10\s.*all.*')
P20AllPattern = re.compile(r'^(?i)P20\s.*all.*')
P30AllPattern = re.compile(r'^(?i)P30\s.*all.*')

#initial = 'fbDocs=10'
#replacement = ['fbDocs=10','fbDocs=20','fbDocs=30','fbDocs=40','fbDocs=50','fbDocs=100']

#initial = 'fbMu=0'
#replacement = ['fbMu=0','fbMu=2500','fbMu=5000','fbMu=7500','fbMu=10000']
#replacement = ['fbmu=0']

initial = 'fbTerms=5'
replacement = ['fbTerms=5','fbTerms=10','fbTerms=20','fbTerms=30','fbTerms=40','fbTerms=50']

#initial = 'fbOrigWeight=0.0'
#replacement = ['fbOrigWeight=0.0','fbOrigWeight=0.2','fbOrigWeight=0.4','fbOrigWeight=0.6','fbOrigWeight=0.8','fbOrigWeight=1.0']

for r in replacement:
	parameter = fileinput.input('/Users/Caesar/Documents/workspace/SearchEngineLab1/parameterFile', inplace=1)
	for i, line in enumerate(parameter):
		sys.stdout.write(line.replace(initial, r))
	print r
	writeFile(r)
	parameter.close()
	initial = r
    ##if i == 4: sys.stdout.write('\n')  # write a blank line after the 5th line

	execute_java('QryEval.class', '/Users/Caesar/Documents/workspace/SearchEngineLab1/parameterFile')

	result =  os.popen('''
		perl -e '
			use LWP::Simple;
			my $fileIn = "/Users/Caesar/Documents/workspace/SearchEngineLab1/HW1-queries-UB.teIn";
			my $url = "http://boston.lti.cs.cmu.edu/classes/11-642/HW/HTS/tes.cgi";
			my $ua = LWP::UserAgent->new();
	   		$ua->credentials("boston.lti.cs.cmu.edu:80", "HTS", "haileiy", "ZTM2YzAw");
			my $result = $ua->post($url,
				   Content_Type => "form-data",
				   Content => [ logtype => "Detailed", infile => [$fileIn],	hwid => "HW4"]);
			my $result = $result->as_string;
	   		$result =~ s/<br>/\n/g;
			print $result;'
		''').read()
	result = result.splitlines()
	win = 0
	lose = 0
	mapAll = 0.0
	for each in result:
		#print each   #re-comment this line to check the correctness
		if MAPPattern.match(each):  # match MAP of each query
			split = each.split()
			MAP = split[2]          # get MAP of each query
			queryID = split[1]      
			#print float(MAP), baseline[queryID]
			if float(MAP) > baseline[queryID]:
				print queryID + "\twin"
				writeFile(queryID + "\twin")      
				win = win + 1	
			elif float(MAP) < baseline[queryID]:
				lose = lose + 1
				print queryID + "\tlose"
				writeFile(queryID + "\tlose") 
			else:
				print queryID + "\tequal"
				writeFile(queryID + "\tequal")
		elif MAPAllPattern.match(each): # match MAP of summary
			split = each.split()
		    #print split[2] #re-comment this line to check the correctness
			mapAll = split[2]           # save it and print it later
		elif P10AllPattern.match(each): # match P10 of summary
			split = each.split()
			print split[2]
			writeFile(split[2])
		elif P20AllPattern.match(each): # match P20 of summary
			split = each.split()
			print split[2]
			writeFile(split[2])
		elif P30AllPattern.match(each): # match P30 of summary
			split = each.split()
			print split[2]
			writeFile(split[2])
	print mapAll                        # print MAP of summary
	writeFile(str(mapAll))
	os.remove(fileIn)
	print str(win)+"/"+str(lose)   	# print win/lose
	writeFile(str(win)+"/"+str(lose))
	writeFile(" ")

	#try:
	#	os.remove(fileIn)
	#except:
	#	pass
