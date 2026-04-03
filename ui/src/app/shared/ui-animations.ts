import { animate, keyframes, query, stagger, style, transition, trigger } from '@angular/animations';

export const enterMotion = trigger('enterMotion', [
  transition(
    ':enter',
    [
      style({
        opacity: 0,
        transform: '{{fromTransform}}'
      }),
      animate(
        '{{duration}}ms {{delay}}ms cubic-bezier(0.2, 0.68, 0.2, 1)',
        style({
          opacity: 1,
          transform: 'translate3d(0, 0, 0)'
        })
      )
    ],
    {
      params: {
        fromTransform: 'translate3d(0, 14px, 0)',
        duration: 520,
        delay: 0
      }
    }
  )
]);

export const listItemMotion = trigger('listItemMotion', [
  transition(
    ':enter',
    [
      style({
        opacity: 0,
        transform: 'translate3d(0, 10px, 0)'
      }),
      animate(
        '{{duration}}ms {{delay}}ms cubic-bezier(0.2, 0.68, 0.2, 1)',
        style({
          opacity: 1,
          transform: 'translate3d(0, 0, 0)'
        })
      )
    ],
    {
      params: {
        duration: 320,
        delay: 0
      }
    }
  )
]);

export const alertMotion = trigger('alertMotion', [
  transition(':enter', [
    animate(
      '420ms cubic-bezier(0.2, 0.68, 0.2, 1)',
      keyframes([
        style({ opacity: 0, transform: 'translate3d(0, -8px, 0)', offset: 0 }),
        style({ opacity: 1, transform: 'translate3d(-8px, 0, 0)', offset: 0.35 }),
        style({ opacity: 1, transform: 'translate3d(6px, 0, 0)', offset: 0.6 }),
        style({ opacity: 1, transform: 'translate3d(0, 0, 0)', offset: 1 })
      ])
    )
  ])
]);

export const staggerChildrenMotion = trigger('staggerChildrenMotion', [
  transition(':enter', [
    query(
      '.stagger-item',
      [
        style({ opacity: 0, transform: 'translate3d(0, 12px, 0) scale(0.985)' }),
        stagger(
          60,
          animate(
            '360ms cubic-bezier(0.2, 0.68, 0.2, 1)',
            style({ opacity: 1, transform: 'translate3d(0, 0, 0) scale(1)' })
          )
        )
      ],
      { optional: true }
    )
  ]),
  transition('* => *', [
    query(
      ':enter',
      [
        style({ opacity: 0, transform: 'translate3d(0, 10px, 0)' }),
        stagger(
          36,
          animate(
            '300ms cubic-bezier(0.2, 0.68, 0.2, 1)',
            style({ opacity: 1, transform: 'translate3d(0, 0, 0)' })
          )
        )
      ],
      { optional: true }
    ),
    query(
      ':leave',
      [
        stagger(
          24,
          animate(
            '180ms cubic-bezier(0.4, 0, 1, 1)',
            style({ opacity: 0, transform: 'translate3d(0, -8px, 0)' })
          )
        )
      ],
      { optional: true }
    )
  ])
]);

export const metricPulseMotion = trigger('metricPulseMotion', [
  transition(':increment', [
    animate(
      '420ms cubic-bezier(0.2, 0.68, 0.2, 1)',
      keyframes([
        style({ transform: 'translate3d(0, 0, 0) scale(1)', offset: 0 }),
        style({ transform: 'translate3d(0, -2px, 0) scale(1.07)', offset: 0.45 }),
        style({ transform: 'translate3d(0, 0, 0) scale(1)', offset: 1 })
      ])
    )
  ]),
  transition(':decrement', [
    animate(
      '360ms cubic-bezier(0.4, 0, 0.2, 1)',
      keyframes([
        style({ transform: 'translate3d(0, 0, 0) scale(1)', opacity: 1, offset: 0 }),
        style({ transform: 'translate3d(0, 1px, 0) scale(0.96)', opacity: 0.86, offset: 0.5 }),
        style({ transform: 'translate3d(0, 0, 0) scale(1)', opacity: 1, offset: 1 })
      ])
    )
  ])
]);

export const revealMotion = trigger('revealMotion', [
  transition(':enter', [
    style({ height: 0, opacity: 0, transform: 'translate3d(0, -8px, 0)' }),
    animate(
      '320ms cubic-bezier(0.2, 0.68, 0.2, 1)',
      style({ height: '*', opacity: 1, transform: 'translate3d(0, 0, 0)' })
    )
  ]),
  transition(':leave', [
    style({ height: '*', opacity: 1, transform: 'translate3d(0, 0, 0)' }),
    animate(
      '220ms cubic-bezier(0.4, 0, 1, 1)',
      style({ height: 0, opacity: 0, transform: 'translate3d(0, -8px, 0)' })
    )
  ])
]);