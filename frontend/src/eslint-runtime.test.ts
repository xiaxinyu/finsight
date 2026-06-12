import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

function runLint(): string {
  try {
    return execFileSync('npm', ['run', 'lint'], {
      cwd: frontendRoot,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    })
  } catch (error) {
    const err = error as { stdout?: string; stderr?: string; message?: string }
    return [err.stdout, err.stderr, err.message].filter(Boolean).join('\n')
  }
}

describe('eslint runtime', () => {
  it('runs lint without stylish formatter crash', () => {
    const output = runLint()

    expect(output).not.toMatch(/util\.styleText is not a function/)
    expect(output).not.toMatch(/formatter is no longer part of core ESLint/)
    expect(output).toMatch(/problems?|error|warning|✖/i)
  })
})
