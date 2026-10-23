#!/usr/bin/env python3
"""
Minecraft Mod Translation File Checker
Compares language files against the main en_us.json file
"""

import json
import sys
from pathlib import Path


def load_json_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: File '{filepath}' not found.")
        return None
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in '{filepath}': {e}")
        return None


def compare_translations(main_file, generated_file, target_file):
    main_data = load_json_file(main_file)
    if main_data is None:
        return

    generated_data = load_json_file(generated_file)
    if generated_data is None:
        return

    target_data = load_json_file(target_file)
    if target_data is None:
        return

    main_keys = set(main_data.keys()).union(set(generated_data.keys()))
    target_keys = set(target_data.keys())

    # Find missing translations (in main but not in target)
    missing_keys = main_keys - target_keys

    # Find extra translations (in target but not in main)
    extra_keys = target_keys - main_keys

    # Display results
    print()
    if missing_keys:
        print(f"Found {len(missing_keys)} Missing translation{'s' if len(missing_keys) != 1 else ''} from {target_file}:")
        for key in sorted(missing_keys):
            # Get the value from whichever dict has it
            value = main_data.get(key) or generated_data.get(key)
            print(f'"{key}": "{value}"')
    else:
        print(f"No missing translations in {target_file}. Great job!")

    print()
    if extra_keys:
        print(f"Found {len(extra_keys)} Extra translation{'s' if len(extra_keys) != 1 else ''} no longer used in {target_file}:")
        for key in sorted(extra_keys):
            print(f'"{key}": NO LONGER USED IN FORESTRY')
    else:
        print(f"No extra translations in {target_file}.")

    print()


def main():
    # Get the directory where this script is located
    script_dir = Path(__file__).parent

    # Build paths relative to the script location
    main_file = script_dir / "en_us.json"
    # Navigate to project root and then to generated folder
    generated_file = script_dir / ".." / ".." / ".." / ".." / ".." / "generated" / "resources" / "assets" / "forestry" / "lang" / "en_us.json"
    generated_file = generated_file.resolve()  # Resolve to absolute path

    # Check if main file exists
    if not main_file.exists():
        print(f"Error: Main translation file 'en_us.json' not found at {main_file}")
        sys.exit(1)

    # Check if generated file exists
    if not generated_file.exists():
        print(f"Error: Generated translation file not found at {generated_file}")
        sys.exit(1)

    # Get target language file from command line args or prompt
    if len(sys.argv) > 1:
        target_file = sys.argv[1]
    else:
        target_file = input("What is your language file?\n> ").strip()

    if not target_file:
        print("Error: No filename provided.")
        sys.exit(1)

    # Convert to Path object if it's just a filename (resolve relative to script dir)
    target_path = Path(target_file)
    if not target_path.is_absolute() and not target_path.exists():
        target_path = script_dir / target_file

    # Run comparison
    compare_translations(str(main_file), str(generated_file), str(target_path))

    input("\nPress Enter to continue...")


if __name__ == "__main__":
    main()
