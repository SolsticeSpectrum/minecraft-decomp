#!/usr/bin/env python3
"""
Convert 3-space indentation to 4-space indentation in patch files.
"""

import sys
import re

def convert_indent(line):
    """Convert 3-space indentation to 4-space indentation in a patch line."""

    # Skip patch metadata lines
    if line.startswith('---') or line.startswith('+++') or line.startswith('@@') or line.startswith('diff '):
        return line

    # Handle context lines (start with space), added lines (+), removed lines (-)
    if line.startswith(' ') or line.startswith('+') or line.startswith('-'):
        prefix = line[0]
        rest = line[1:]

        # Count leading spaces after the prefix
        leading_spaces = 0
        for char in rest:
            if char == ' ':
                leading_spaces += 1
            else:
                break

        # Convert 3-space indent to 4-space indent
        # Each indent level is 3 spaces, convert to 4
        indent_levels = leading_spaces // 3
        remainder = leading_spaces % 3

        new_spaces = (indent_levels * 4) + remainder
        new_rest = ' ' * new_spaces + rest[leading_spaces:]

        return prefix + new_rest

    return line

def process_patch_file(input_path, output_path=None):
    """Process a patch file and convert indentation."""

    if output_path is None:
        output_path = input_path

    with open(input_path, 'r') as f:
        lines = f.readlines()

    converted_lines = []
    for line in lines:
        # Preserve line endings
        if line.endswith('\n'):
            converted = convert_indent(line[:-1]) + '\n'
        else:
            converted = convert_indent(line)
        converted_lines.append(converted)

    with open(output_path, 'w') as f:
        f.writelines(converted_lines)

    print(f"Converted {input_path} -> {output_path}")

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: fix_patch_indent.py <patch_file> [output_file]")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None

    process_patch_file(input_file, output_file)
