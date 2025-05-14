<?php
/**
Copyright (c) 2021 Steve Hannah

Permission is hereby granted, free of charge,
to any person obtaining a copy of this software and
associated documentation files (the "Software"), to
deal in the Software without restriction, including
without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom
the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

/**
 * This is a wrapper around the Batsh Compiler at https://batsh.org/.
 *
 * Usage:
 * php batsh.php path/to/script.batsh
 *
 * This will compile the script at path/to/script.batsh into both .bat and .sh formats.  The output will be written
 * to the build directory.
 *
 */
define('BATSCH_COMPILER_URL', 'https://batsh.org/compile');
function compile($target, $file) {
    $ch = curl_init();
    $code = file_get_contents($file);
    curl_setopt($ch, CURLOPT_URL, BATSCH_COMPILER_URL);
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, "code=".urlencode($code)."&target=$target");
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    $server_output = curl_exec($ch);
    $response = curl_exec($ch);
    $error    = curl_error($ch);
    $errno    = curl_errno($ch);
    if (is_resource($ch)) {
        curl_close($ch);
    }
    if (0 !== $errno) {
        throw new RuntimeException($error, $errno);
    }
    $json = json_decode($response, true);
    if (!isset($json['code'])) {
        echo "Compilation Error: ";
        print_r($json);
        return null;
    }
    return $json['code'];
}

if (!@$argv) {
    die("CLI usage only");
    
}
$file = $argv[1];

if (!is_readable($file)) {
    die("File $file not readable or not found");
}
if (!preg_match('/\.batsh$/', $file)) {
    die("Only .batsh files can be compiled with this compiler.  Found $file");
}

$dir = 'build';
if (!file_exists($dir)) {
    mkdir($dir);
}

$base = substr(basename($file), 0, -6);
$bashOut = $dir . DIRECTORY_SEPARATOR . $base . '.sh';
$batOut = $dir . DIRECTORY_SEPARATOR . $base . '.bat';
$bashContent = compile('bash', $file);
if (!isset($bashContent)) exit(1);
$batContent = compile('winbat', $file);
if (!isset($batContent)) exit(1);
echo "Compiling to $bashOut\n";
file_put_contents($bashOut, "#!/bin/bash\n".$bashContent);
echo "Compiling to $batOut\n";
file_put_contents($batOut, $batContent);


?>