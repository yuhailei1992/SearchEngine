./make.sh
java WriteParam 0
./exec.sh
perl driver.pl > results/result.txt
java ParseResult 0

