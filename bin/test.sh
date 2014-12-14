./make.sh
java WriteParam 0
./exec.sh
perl driver.pl > results/result.txt
java ParseResult 0

java WriteParam 2500
./exec.sh
perl driver.pl > results/result.txt
java ParseResult 2500

java WriteParam 5000
./exec.sh
perl driver.pl > results/result.txt
java ParseResult 5000

java WriteParam 7500
./exec.sh
perl driver.pl > results/result.txt
java ParseResult 7500

java WriteParam 10000
./exec.sh
perl driver.pl > results/result.txt
java ParseResult 10000
