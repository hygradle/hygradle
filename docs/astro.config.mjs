// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
    site: "https://docs.hygradle.dev",
    base: "/",
	integrations: [
		starlight({
			title: 'Hygradle',
            logo: {
              src: './src/assets/hygradle.svg',
              replacesTitle: true,
            },
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/remi-gelinas/hygradle' }],
			sidebar: [
				{
					label: 'Guides',
					items: [
						// Each item here is one entry in the navigation menu.
						{ label: 'Example Guide', slug: 'guides/example' },
					],
				},
				{
					label: 'Reference',
					autogenerate: { directory: 'reference' },
				},
			],
		}),
	],
});
