#!/usr/bin/env bash
set -euo pipefail

currentVersion=$(./mvnw help:evaluate \
    -Dexpression=project.version \
    -q \
    -DforceStdout 2>/dev/null)

echo "Current Version is $currentVersion"

# Split off suffix like -SNAPSHOT (if present)
versionCore="${currentVersion%%-*}"
suffix=""
if [[ "$currentVersion" == *-* ]]; then
    suffix="-${currentVersion#*-}"
fi

IFS='.' read -r major minor patch <<< "$versionCore"

echo "Select version bump:"
select choice in "Bump Major" "Bump Minor" "Bump Patch" "Free Text"; do
    case $choice in
        "Bump Major")
            newVersion="$((major + 1)).0.0${suffix}"
            break
            ;;
        "Bump Minor")
            newVersion="${major}.$((minor + 1)).0${suffix}"
            break
            ;;
        "Bump Patch")
            newVersion="${major}.${minor}.$((patch + 1))${suffix}"
            break
            ;;
        "Free Text")
            read -rp "Enter new version: " newVersion
            break
            ;;
        *)
            echo "Invalid option. Please select 1-4."
            ;;
    esac
done

echo "New version will be: $newVersion"
read -rp "Proceed? [y/N] " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

./mvnw -B versions:set -DgenerateBackupPoms=false -DnewVersion="$newVersion"
