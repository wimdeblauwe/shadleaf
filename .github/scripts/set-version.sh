#!/usr/bin/env bash
# Sets the version of the library (parent + starter) and of the standalone samples, including the Shadleaf version the
# samples depend on. Used by the release workflow; works locally too: .github/scripts/set-version.sh 1.0.0
set -euo pipefail

version=${1:?usage: set-version.sh <version>}
root=$(cd "$(dirname "$0")/../.." && pwd)
versions_plugin=org.codehaus.mojo:versions-maven-plugin:2.22.0

mvn -B -q -f "$root/pom.xml" "$versions_plugin:set" -DnewVersion="$version" -DgenerateBackupPoms=false -DprocessAllModules=true

for sample in "$root"/samples/*/pom.xml; do
  mvn -B -q -f "$sample" "$versions_plugin:set" -DnewVersion="$version" -DgenerateBackupPoms=false
  # A plain property: versions:set-property would look the new version up in a repository, where it is not yet.
  sed -i.bak -E "s|<shadleaf\.version>[^<]*</shadleaf\.version>|<shadleaf.version>$version</shadleaf.version>|" "$sample"
  rm "$sample.bak"
  grep -q "<shadleaf.version>$version</shadleaf.version>" "$sample" || { echo "shadleaf.version not set in $sample" >&2; exit 1; }
done
