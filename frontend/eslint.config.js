import tsParser from '@typescript-eslint/parser'
import vue from 'eslint-plugin-vue'

export default [
  {
    ignores: ['dist/**', 'node_modules/**', 'src/auto-imports.d.ts', 'src/components.d.ts']
  },
  ...vue.configs['flat/essential'],
  {
    files: ['**/*.ts', '**/*.vue'],
    languageOptions: {
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        parser: tsParser
      }
    }
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tsParser
    }
  }
]
